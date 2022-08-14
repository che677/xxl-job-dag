package com.xxl.job.dp.core;
import com.xxl.job.dp.common.reject.RejectProxy;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dynamic ThreadPoolExecutor inherits DtpLifecycleSupport, and extends some features.
 *
 * @author: yanhom
 * @since 1.0.0
 **/
public class DtpExecutor extends DtpLifecycleSupport {

    /**
     * Total reject count.
     */
    private final AtomicInteger rejectCount = new AtomicInteger(0);

    /**
     * RejectHandler name.
     */
    private String rejectHandlerName;

    /**
     * Simple Business alias Name of Dynamic ThreadPool. Use for notify.
     */
    private String theadPoolAliasName;

    /**
     * If pre start all core threads.
     */
    private boolean preStartAllCoreThreads;

    /**
     * Task execute timeout, unit (ms), just for statistics.
     */
    private long runTimeout;

    /**
     * Task queue wait timeout, unit (ms), just for statistics.
     */
    private long queueTimeout;

    /**
     * Count run timeout tasks.
     */
    private final AtomicInteger runTimeoutCount = new AtomicInteger();

    /**
     * Count queue wait timeout tasks.
     */
    private final AtomicInteger queueTimeoutCount = new AtomicInteger();

    public DtpExecutor(int corePoolSize,
                       int maximumPoolSize,
                       long keepAliveTime,
                       TimeUnit unit,
                       BlockingQueue<Runnable> workQueue,
                       ThreadFactory threadFactory,
                       RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.rejectHandlerName = handler.getClass().getSimpleName();
        // 从配置文件中读取是否开启reject监控，然后采用动态代理的方式来执行
        RejectedExecutionHandler rejectedExecutionHandler = RejectProxy.getProxy(handler, true);
        setRejectedExecutionHandler(rejectedExecutionHandler);
    }

    @Override
    public void execute(Runnable command) {
        super.execute(command);
    }

    @Override
    protected void initialize() {
        if (preStartAllCoreThreads) {
            prestartAllCoreThreads();
        }
    }

    public void incRejectCount(int count) {
        rejectCount.addAndGet(count);
    }

    public int getRejectCount() {
        return rejectCount.get();
    }

    public String getQueueName() {
        return getQueue().getClass().getSimpleName();
    }

    public int getQueueCapacity() {
        int capacity = getQueue().size() + getQueue().remainingCapacity();
        return capacity < 0 ? Integer.MAX_VALUE : capacity;
    }

    public String getRejectHandlerName() {
        return rejectHandlerName;
    }

    public void setRejectHandlerName(String rejectHandlerName) {
        this.rejectHandlerName = rejectHandlerName;
    }

    public void setPreStartAllCoreThreads(boolean preStartAllCoreThreads) {
        this.preStartAllCoreThreads = preStartAllCoreThreads;
    }

    public void setRunTimeout(long runTimeout) {
        this.runTimeout = runTimeout;
    }

    public int getRunTimeoutCount() {
        return runTimeoutCount.get();
    }

    public int getQueueTimeoutCount() {
        return queueTimeoutCount.get();
    }

    public void setQueueTimeout(long queueTimeout) {
        this.queueTimeout = queueTimeout;
    }

    /**
     * In order for the field can be assigned by reflection.
     * @param allowCoreThreadTimeOut allowCoreThreadTimeOut
     */
    public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        allowCoreThreadTimeOut(allowCoreThreadTimeOut);
    }

    public String getTheadPoolAliasName() {
        return theadPoolAliasName;
    }

    public void setTheadPoolAliasName(String theadPoolAliasName) {
        this.theadPoolAliasName = theadPoolAliasName;
    }
}
