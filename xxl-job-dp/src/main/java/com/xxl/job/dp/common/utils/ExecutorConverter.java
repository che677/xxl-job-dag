package com.xxl.job.dp.common.utils;

import com.xxl.job.dp.common.enumerate.QueueTypeEnum;
import com.xxl.job.dp.common.property.DtpMainProp;
import com.xxl.job.dp.common.property.ThreadPoolProperties;
import com.xxl.job.dp.core.DtpExecutor;
import com.xxl.job.dp.core.ThreadPoolBuilder;
import lombok.val;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ExecutorConverter related
 *
 * @author: yanhom
 * @since 1.0.0
 **/
public class ExecutorConverter {

    private ExecutorConverter() {}

    public static DtpMainProp convert(DtpExecutor dtpExecutor) {
        DtpMainProp mainProp = new DtpMainProp();
        mainProp.setThreadPoolName(dtpExecutor.getThreadPoolName());
        mainProp.setCorePoolSize(dtpExecutor.getCorePoolSize());
        mainProp.setMaxPoolSize(dtpExecutor.getMaximumPoolSize());
        mainProp.setKeepAliveTime(dtpExecutor.getKeepAliveTime(TimeUnit.SECONDS));
        mainProp.setQueueType(dtpExecutor.getQueueName());
        mainProp.setQueueCapacity(dtpExecutor.getQueueCapacity());
        mainProp.setRejectType(dtpExecutor.getRejectHandlerName());
        mainProp.setAllowCoreThreadTimeOut(dtpExecutor.allowsCoreThreadTimeOut());
        return mainProp;
    }

    public static DtpExecutor revert(ThreadPoolProperties properties) {
        return ThreadPoolBuilder.newBuilder()
                .threadPoolName(properties.getThreadPoolName())
                .corePoolSize(properties.getCorePoolSize())
                .maximumPoolSize(properties.getMaximumPoolSize())
                .keepAliveTime(properties.getKeepAliveTime())
                .timeUnit(TimeUnit.MILLISECONDS)
                .workQueue(properties.getQueueType(), properties.getQueueCapacity(), false, null)
                .waitForTasksToCompleteOnShutdown(true)
                .awaitTerminationSeconds(5)
                .rejectedExecutionHandler(properties.getRejectedHandlerType())
                .buildDynamic();
    }

    public static DtpMainProp ofSimple(String name, int corePoolSize, int maxPoolSize, long keepAliveTime) {
        DtpMainProp mainProp = new DtpMainProp();
        mainProp.setThreadPoolName(name);
        mainProp.setCorePoolSize(corePoolSize);
        mainProp.setMaxPoolSize(maxPoolSize);
        mainProp.setKeepAliveTime(keepAliveTime);
        return mainProp;
    }
}
