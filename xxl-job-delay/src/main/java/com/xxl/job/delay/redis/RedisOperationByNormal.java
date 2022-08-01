package com.xxl.job.delay.redis;

import com.xxl.job.delay.common.Args;
import com.xxl.job.delay.utils.RedisKeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.RedisClientInfo;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @Description redis的操作类; 线程不安全; 不可使用
 * @Author shirenchuang
 * @Date 2019/8/1 3:12 PM
 **/
public class RedisOperationByNormal implements RedisOperation {

    private static final Logger logger = LoggerFactory.getLogger(RedisOperationByNormal.class);

    protected RedisTemplate redisTemplate;

    public RedisOperationByNormal(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    //新增一个Job
    @Override
    public void addJob(String topic, Args arg, long runTimeMillis){
        List<String> keys = new ArrayList<>();
        // keys中有 hashtable是Redis_Delay_Table, K: TOPIC:ID V: arg
        // bucket是 RD_ZSET_BUCKET， Member: TOPIC:ID,  Score: runTimeMilis
        keys.add(RedisKeyUtil.getDelayQueueTableKey());
        keys.add(RedisKeyUtil.getBucketKey());
        DefaultRedisScript redisScript =new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/addJob.lua")));
        redisTemplate.execute(redisScript,keys,RedisKeyUtil.getTopicId(topic, arg.getId()),arg,runTimeMillis);
        logger.info("新增延时任务:Topic:{};id:{},runTimeMillis:{},Args={}",topic,arg.getId(),runTimeMillis,arg.toString());
    }

    @Override
    public void retryJob(String topic, String id, Object content){
        // 更新hash表里的数据，重新推送topic信息进入对应的list中等待消费
        List<String> keys = new ArrayList<>();
        keys.add(RedisKeyUtil.getDelayQueueTableKey());
        keys.add(RedisKeyUtil.getTopicListKey(topic));
        DefaultRedisScript redisScript =new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/retryJob.lua")));
        redisTemplate.execute(redisScript,keys,RedisKeyUtil.getTopicId(topic, id),content);
    }

    //删除一个Job
    @Override
    public void deleteJob(String topic, String id){
        // 删除hash表中的消息, 删除bucket中的消息
        List<String> keys =  new ArrayList<>();
        keys.add(RedisKeyUtil.getDelayQueueTableKey());
        keys.add(RedisKeyUtil.getBucketKey());
        DefaultRedisScript redisScript =new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/deleteJob.lua")));
        redisTemplate.execute(redisScript,keys,RedisKeyUtil.getTopicId(topic, id));
    }

    /**
     * 从zset搬运到list
     * 做一次搬运操作，然后返回搬运完之后的 队首元素的score，也就是下一个要搬运的消息，这样就可以实现wait notify机制了
     * 如果搬运之后没有了元素则返回Long.MAX_VALUE
     * @return
     */
    @Override
    public long moveAndRtTopScore(){
        List<String> keys = new ArrayList<>(2);
        //移动到的待消费列表key  这里是每个topic_list的前缀，还没有组合上topic
        keys.add(RedisKeyUtil.getTopicListPreKey());
        //被移动的zset
        keys.add(RedisKeyUtil.getBucketKey());
        DefaultRedisScript<String> redisScript =new DefaultRedisScript<>();
        redisScript.setResultType(String.class);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/moveAndRtTopScore.lua")));
        // 传入topic_list前缀，传入zset名称，传入当前时间（因为lua获取当前时间只能用一次）
        String newTime = (String) redisTemplate.execute(redisScript,redisTemplate.getValueSerializer(),
                redisTemplate.getStringSerializer(),keys,System.currentTimeMillis());
        //logger.info("执行一次移动操作用时:{} ",System.currentTimeMillis()-before);
        if(StringUtils.isEmpty(newTime))return Long.MAX_VALUE;
        return Long.parseLong(newTime);
    }

    //阻塞获取List中的元素
    @Override
    public Object BLPOP(String topic){
        String topicId = BLPOPKey(topic);
        if(topicId == null)return null;
        return getJob(topicId);
    }

    /**
     * 阻塞超时会返回null,默认超时时间是5min
     * n秒之内没有获取到数据则断开连接
     * @param topic
     * @return
     */
    @Override
    public String BLPOPKey(String topic){
        Object object =  BLPOP(RedisKeyUtil.getTopicListKey(topic),5*60*1000);
        if(object==null)return null;
        return object.toString();
    }
    @Override
    public String BLPOP(String key,long timeout){
        Object object = redisTemplate.opsForList().leftPop(key,timeout,TimeUnit.MILLISECONDS);
        if(object==null)return null;
        return object.toString();
    }

    /**
     * 一次最多能取maxGet个元素，topic_list是目标列表
     * @param topic
     * @param maxGet
     * @return
     */
    @Override
    public List<String> lrangeAndLTrim(String topic, int maxGet) {
        //lua 是以0为开始
        maxGet = maxGet-1;
        List<String> keys = new ArrayList<>(1);
        // topic_list的名字
        keys.add(RedisKeyUtil.getTopicListKey(topic));
        DefaultRedisScript<Object> redisScript =new DefaultRedisScript<>();
        redisScript.setResultType(Object.class);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/lrangAndLTrim.lua")));
        Object values ;
        try {
            values = redisTemplate.execute(redisScript, redisTemplate.getValueSerializer(),
                    redisTemplate.getStringSerializer(), keys, maxGet);
        }catch (RedisSystemException e){
            //redistemplate 有个bug  没有获取到数据的时候报空指针
            if(e.getCause() instanceof NullPointerException){
                return null;
            }else {
                logger.error("lrangeAndLTrim 操作异常;{}", e);
                throw e;
            }
        }
        List<String> list = (List<String>)values;
        return list;
    }

    /****
     *     查询REDIS_DELAY_TABLE 中的Job详情
     */
    @Override
    public Args getJob(String topicId){
        Object args =  redisTemplate.opsForHash().get(RedisKeyUtil.getDelayQueueTableKey(),topicId);
        if(args == null)return null;
        return (Args)args;
    }

    @Override
    public List<RedisClientInfo> getThisMachineAllBlpopClientList(){
        List<RedisClientInfo> list  =  redisTemplate.getClientList();
        return list;
    }

    @Override
    public void killClient(List<String> clients) {
        clients.forEach((a)->{
            String address[] = a.split(":");
            redisTemplate.killClient(address[0],Integer.parseInt(address[1]));
        });
    }

}
