package com.xxl.job.delay.threads;

import com.xxl.job.delay.redis.RedisOperation;
import com.xxl.job.delay.utils.LockUtil;
import com.xxl.job.delay.utils.NextTimeHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Description 搬运线程
 * @Author shirenchuang
 * @Date 2019/8/4 10:18 AM
 **/
public class Move2ReadyThread {

    private static final Logger logger = LoggerFactory.getLogger(Move2ReadyThread.class);

    private static Move2ReadyThread instance = new Move2ReadyThread();
    public static Move2ReadyThread getInstance(){
        return instance;
    }

    private volatile boolean toStop = false;


    /** 搬运线程:  将 bucket中的延迟有序队列zSet尝试move到待消费队列List**/
    private  final ExecutorService BUCKET_MOVE_TO_LIST = Executors.newSingleThreadExecutor();
    /**
     * 搬运线程，这里是单线程搬运元素，从zset中搬运到topic_list中
     * 一秒执行一次，会有wait notify机制
     */
    public void runMove2ReadyThread(RedisOperation redisOperation){

        BUCKET_MOVE_TO_LIST.execute(()->{

            while (!toStop){
                // 睡一秒钟:  避免QPS过高,并且频繁的有更小的的时间戳导致执行
                // 搬运操作频率过高没有必要(而且一次搬运是搬运多个元素的,一秒钟之内更新最小的时间戳就行了)
                // 策略是: 最慢每分钟执行一次; 最快一秒一次;
                // 如果当前时间还没到 nextTime执行时间，就wait；
                // 否则就开始从zset中搬运元素到topic_list中
                // 搬运元素之后，再重置下次执行时间
                try {
                    Thread.sleep(1000);
                    long timeout;
                    if((timeout  = (NextTimeHolder.nextTime.get()-System.currentTimeMillis()))>0){
                        try {
                            synchronized (LockUtil.lock) {
                                LockUtil.lock.wait(timeout);
                            }
                        } catch (InterruptedException e) {
                            logger.warn("runMove2ReadyThread wait被打断");
                            e.printStackTrace();
                        }
                    }else {
                        //move
                        long score = redisOperation.moveAndRtTopScore();
                        //重新设置nextTime值;这里不调用tryUpdate来更新;而是直接set;以这里的为准
                        NextTimeHolder.nextTime.set(score);
                    }
                } catch (InterruptedException e) {
                    //停机打断睡眠
                }catch (Exception ec){
                    //redis 网络挂掉  睡5s
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    public void toStop() {
        logger.info("搬运线程关闭.....");
        toStop = true;
        BUCKET_MOVE_TO_LIST.shutdown();
    }
}
