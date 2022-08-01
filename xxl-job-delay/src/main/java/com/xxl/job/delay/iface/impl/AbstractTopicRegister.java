package com.xxl.job.delay.iface.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.xxl.job.delay.common.Args;
import com.xxl.job.delay.core.RedisDelayQueueContext;
import com.xxl.job.delay.iface.CallBack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Description TODO
 * @Author shirenchuang
 * @Date 2019/7/31 9:05 AM
 **/
public abstract class AbstractTopicRegister <T extends Args> implements CallBack<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTopicRegister.class);

    /**初始化的时候自动注册**/
    public AbstractTopicRegister() {
        RedisDelayQueueContext.addTopic(getTopic(),this);
    }

    /**线程池默认核心数**/
    private int corePoolSize = 2;

    /**线程池默认最大线程数**/
    private int maxPoolSize = 20;

    /**回调方法默认超时时间 单位毫秒*/
    private int methodTimeout = 6000;

    /**每次去redis的待消费列表读取的最大元素个数**/
    private int lrangMaxCount = 100;

    // SynchronousQueue是零容量的，所以本线程池的情况就是，所有的请求进来之后，都会直接生成任务，直到到达最大线程数
    private ThreadPoolExecutor TOPIC_THREADS = new ThreadPoolExecutor(
            getCorePoolSize(),
            getMaxPoolSize(),
            60000,
            TimeUnit.MILLISECONDS,
            new SynchronousQueue(),
            new ThreadFactoryBuilder().setDaemon(true).setNameFormat(getTopic()+"-%d").build(),
            new ThreadPoolExecutor.AbortPolicy()
    );

    final public ThreadPoolExecutor getTOPIC_THREADS() {
        return TOPIC_THREADS;
    }

    //注册Topic
    public abstract String getTopic();

    @Override
    public void retryOutTimes(T t) {
        logger.error("警告! Topic:{},Id:{} 已经重试仍然失败~ 请大佬关注一下 ",getTopic(),t.getId());
    }

    /**
     * 设置核心线程池数量  可重写这个方法
     * @return
     */
    public int getCorePoolSize(){
        return corePoolSize;
    }

    /**
     * 设置线程池最大线程数量
     * @return
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * 获取 回调方法的超时时间 可重写这个方法 单位毫秒;
     * @return
     */
    public int getMethodTimeout() {
        return methodTimeout;
    }

    final public int getLrangMaxCount() {
        return lrangMaxCount;
    }
}
