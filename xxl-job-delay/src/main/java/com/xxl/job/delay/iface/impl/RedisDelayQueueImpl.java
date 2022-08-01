package com.xxl.job.delay.iface.impl;

import com.xxl.job.delay.common.Args;
import com.xxl.job.delay.core.RedisDelayQueueContext;
import com.xxl.job.delay.iface.RedisDelayQueue;
import com.xxl.job.delay.redis.RedisOperation;
import com.xxl.job.delay.utils.NextTimeHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * @Description 提供给客户端使用的 延迟队列操作
 * @Author shirenchuang
 * @Date 2019/7/30 5:33 PM
 **/
public  class RedisDelayQueueImpl implements RedisDelayQueue {
    private static final Logger logger = LoggerFactory.getLogger(RedisDelayQueueContext.class);
    // redis操作类
    private RedisOperation redisOperation;
    // k是topic名称，v是AbstractTopicRegister类，代表着一种topic,里面包含了线程池；这里就是所有主题的集合
    private ConcurrentHashMap<String, AbstractTopicRegister> topicRegisterHolder;
    // 直接用的是RedisDelayQueueContext中的线程池
    private ExecutorService executor;

    public RedisDelayQueueImpl(RedisOperation redisOperation, ConcurrentHashMap<String, AbstractTopicRegister> topicRegisterHolder, ExecutorService executor) {
        this.redisOperation = redisOperation;
        this.topicRegisterHolder = topicRegisterHolder;
        this.executor = executor;
    }

    @Override
    public void add(Args args,String topic,long runTimeMillis) {
        executor.execute(() ->addJob(args, topic, runTimeMillis));
    }

    @Override
    public void add(Args args, long delayTimeMillis, String topic){
        executor.execute(() ->addJob(args, delayTimeMillis, topic));
    }

    private void addJob(Args args, long delayTimeMillis, String topic) {
        preCheck(args,topic,null,delayTimeMillis);
        long runTimeMillis = System.currentTimeMillis()+delayTimeMillis;
        redisOperation.addJob(topic,args,runTimeMillis);
        //尝试更新下次的执行时间
        NextTimeHolder.tryUpdate(runTimeMillis);
    }
    private void addJob(Args args, String topic, long runTimeMillis) {
        preCheck(args,topic,runTimeMillis,null);
        redisOperation.addJob(topic,args,runTimeMillis);
        //尝试更新下次的执行时间
        NextTimeHolder.tryUpdate(runTimeMillis);
    }
    private void preCheck(Args args,String topic,Long runTimeMillis,Long delayTimeMillis) {
        if(checkStringEmpty(topic)||
                checkStringEmpty(args.getId())){
            throw new RuntimeException("未设置Topic或者Id!");
        }
        if(runTimeMillis==null){
            if(delayTimeMillis==null){
                throw new RuntimeException("未设置延迟执行时间!");
            }
        }
        if(topic.contains(":")){
            throw new RuntimeException("Topic 不能包含特殊字符 :  !");
        }
        //check topic exist
//        if(!checkTopicExist(topic)){
//            throw new RuntimeException("Topic未注册!");
//        }
    }

    @Override
    public void delete(String topic, String id) {
        executor.execute(() ->redisOperation.deleteJob(topic, id));
        logger.info("删除延时任务:Topic:{},id：{}",topic,id);
    }

    private boolean checkStringEmpty(String string){
        return string==null||string.length()==0;
    }

    public  boolean checkTopicExist(String topic){
        for(Map.Entry<String, AbstractTopicRegister> entry: topicRegisterHolder.entrySet()) {
            if(entry.getKey().equals(topic)){
                return true;
            }
        }
        return false;
    }
}
