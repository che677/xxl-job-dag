package com.xxl.job.dp.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

/**
 * DtpLifecycleSupport which mainly implements Spring bean's lifecycle methods,
 * mimics spring internal thread pool {@link ThreadPoolTaskExecutor}.
 *
 * @author: yanhom
 * @since 1.0.3
 **/
public abstract class DtpLifecycleSupport extends ThreadPoolExecutor implements InitializingBean{

    /**
     * Uniquely identifies.
     */
    protected String threadPoolName;

    /**
     * Whether to wait for scheduled tasks to complete on shutdown,
     * not interrupting running tasks and executing all tasks in the queue.
     */
    protected boolean waitForTasksToCompleteOnShutdown = false;

    /**
     * The maximum number of seconds that this executor is supposed to block
     * on shutdown in order to wait for remaining tasks to complete their execution
     * before the rest of the container continues to shut down.
     */
    protected int awaitTerminationSeconds = 0;

    public DtpLifecycleSupport(int corePoolSize,
                               int maximumPoolSize,
                               long keepAliveTime,
                               TimeUnit unit,
                               BlockingQueue<Runnable> workQueue,
                               ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public void setWaitForTasksToCompleteOnShutdown(boolean waitForTasksToCompleteOnShutdown) {
        this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown;
    }

    public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
        this.awaitTerminationSeconds = awaitTerminationSeconds;
    }

    public void setThreadPoolName(String threadPoolName) {
        this.threadPoolName = threadPoolName;
    }

    public String getThreadPoolName() {
        return threadPoolName;
    }

    @Override
    public void afterPropertiesSet() {
        initialize();
    }

    /**
     * Initialize, do sth.
     */
    protected abstract void initialize();

}
