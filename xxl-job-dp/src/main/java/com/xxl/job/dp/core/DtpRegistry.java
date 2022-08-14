package com.xxl.job.dp.core;

import com.google.common.collect.Lists;
import com.xxl.job.dp.common.enumerate.QueueTypeEnum;
import com.xxl.job.dp.common.exception.DtpException;
import com.xxl.job.dp.common.property.DtpMainProp;
import com.xxl.job.dp.common.property.DtpProperties;
import com.xxl.job.dp.common.property.ThreadPoolProperties;
import com.xxl.job.dp.common.reject.RejectProxy;
import com.xxl.job.dp.common.utils.ExecutorConverter;
import com.xxl.job.dp.queue.VariableLinkedBlockingQueue;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core Registry, which keeps all registered Dynamic ThreadPoolExecutors.
 *
 * @author: yanhom
 * @since 1.0.0
 **/
@Slf4j
@Data
public class DtpRegistry{

    /**
     * Maintain all automatically registered and manually registered DtpExecutors.
     */
    private static final Map<String, DtpExecutor> DTP_REGISTRY = new ConcurrentHashMap<>();

    private static DtpProperties dtpProperties;

    public static DtpProperties getDtpProperties(){
        return dtpProperties;
    }

    public static void setDtpProperties(DtpProperties dtpProperties) {
        DtpRegistry.dtpProperties = dtpProperties;
    }

    /**
     * Get all DtpExecutor names.
     *
     * @return executor names
     */
    public static List<String> listAllDtpNames() {
        return Lists.newArrayList(DTP_REGISTRY.keySet());
    }

    /**
     * Register a DtpExecutor.
     *
     * @param executor the newly created DtpExecutor instance
     */
    public static void registerDtp(DtpExecutor executor) {
        DTP_REGISTRY.putIfAbsent(executor.getThreadPoolName(), executor);
    }


    /**
     * Get Dynamic ThreadPoolExecutor by thread pool name.
     *
     * @param name the name of dynamic thread pool
     * @return the managed DtpExecutor instance
     */
    public static DtpExecutor getDtpExecutor(final String name) {
        val executor= DTP_REGISTRY.get(name);
        if (Objects.isNull(executor)) {
            log.error("Cannot find a specified dtpExecutor, name: {}", name);
            throw new DtpException("Cannot find a specified dtpExecutor, name: " + name);
        }
        return executor;
    }

    /**
     * Refresh while the listening configuration changed.
     *
     * @param properties the main properties that maintain by config center
     */
    public static void refresh(DtpProperties properties) {
        if (Objects.isNull(properties) || ObjectUtils.isEmpty(properties.getExecutors())) {
            log.warn("DynamicTp refresh, empty threadPoolProperties.");
            return;
        }
        properties.getExecutors().forEach(x -> {
            if (StringUtils.isEmpty(x.getThreadPoolName())) {
                log.warn("DynamicTp refresh, threadPoolName must not be empty.");
                return;
            }
            val dtpExecutor = DTP_REGISTRY.get(x.getThreadPoolName());
            if (Objects.isNull(dtpExecutor)) {
                log.warn("DynamicTp refresh, cannot find specified dtpExecutor, name: {}."+ x.getThreadPoolName());
                return;
            }
            refresh(dtpExecutor, x);
        });
    }
    // 前者是当前已经在内存中的线程池的信息，后者是从zk中读取到的变更信息
    private static void refresh(DtpExecutor executor, ThreadPoolProperties properties) {

        if (properties.getCorePoolSize() < 0 ||
                properties.getMaximumPoolSize() <= 0 ||
                properties.getMaximumPoolSize() < properties.getCorePoolSize() ||
                properties.getKeepAliveTime() < 0) {
            log.error("DynamicTp refresh, invalid parameters exist, properties: {}", properties);
            return;
        }

        if (executor.getMaximumPoolSize() < properties.getCorePoolSize()) {
            log.error("DynamicTp refresh, new corePoolSize [{}] cannot greater than current maximumPoolSize [{}].",
                    properties.getCorePoolSize(), executor.getMaximumPoolSize());
            return;
        }

        DtpMainProp oldProp = ExecutorConverter.convert(executor);
        doRefresh(executor, properties);
        DtpMainProp newProp = ExecutorConverter.convert(executor);
        if (oldProp.equals(newProp)) {
            log.warn("DynamicTp refresh, main properties of [{}] have not changed." + executor.getThreadPoolName());
            return;
        }
        doRefresh(executor, properties);
    }

    private static void doRefresh(DtpExecutor dtpExecutor, ThreadPoolProperties properties) {

        if (!Objects.equals(dtpExecutor.getCorePoolSize(), properties.getCorePoolSize())) {
            dtpExecutor.setCorePoolSize(properties.getCorePoolSize());
            log.info("已修改核心线程数: "+dtpExecutor.getCorePoolSize());
        }

        if (!Objects.equals(dtpExecutor.getMaximumPoolSize(), properties.getMaximumPoolSize())) {
            dtpExecutor.setMaximumPoolSize(properties.getMaximumPoolSize());
            log.info("已修改最大线程数:  "+ dtpExecutor.getMaximumPoolSize());
        }

        if (!Objects.equals(dtpExecutor.getKeepAliveTime(properties.getUnit()), properties.getKeepAliveTime())) {
            dtpExecutor.setKeepAliveTime(properties.getKeepAliveTime(), properties.getUnit());
        }

        if (!Objects.equals(dtpExecutor.allowsCoreThreadTimeOut(), properties.isAllowCoreThreadTimeOut())) {
            dtpExecutor.allowCoreThreadTimeOut(properties.isAllowCoreThreadTimeOut());
        }

        // update reject handler
        if (!Objects.equals(dtpExecutor.getRejectHandlerName(), properties.getRejectedHandlerType())) {
            dtpExecutor.setRejectedExecutionHandler(RejectProxy.getProxy(properties.getRejectedHandlerType(), dtpProperties.isRejectAlarm()));
            dtpExecutor.setRejectHandlerName(properties.getRejectedHandlerType());
        }

        // update Alias Name
        if (!Objects.equals(dtpExecutor.getTheadPoolAliasName(), properties.getTheadPoolAliasName())) {
            dtpExecutor.setTheadPoolAliasName(properties.getTheadPoolAliasName());
        }

        // update work queue
        if (canModifyQueueProp(properties)) {
            val blockingQueue = dtpExecutor.getQueue();
            if (!Objects.equals(dtpExecutor.getQueueCapacity(), properties.getQueueCapacity())) {
                if (blockingQueue instanceof VariableLinkedBlockingQueue) {
                    ((VariableLinkedBlockingQueue<Runnable>) blockingQueue).setCapacity(properties.getQueueCapacity());
                    log.info("已修改队列容量: "+ properties.getQueueCapacity());
                } else {
                    log.error("DynamicTp refresh, the blockingqueue capacity cannot be reset, dtpName: {}, queueType {}",
                            dtpExecutor.getThreadPoolName(), dtpExecutor.getQueueName());
                }
            }
        }

        dtpExecutor.setWaitForTasksToCompleteOnShutdown(properties.isWaitForTasksToCompleteOnShutdown());
        dtpExecutor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
        dtpExecutor.setPreStartAllCoreThreads(properties.isPreStartAllCoreThreads());
        dtpExecutor.setRunTimeout(properties.getRunTimeout());
        dtpExecutor.setQueueTimeout(properties.getQueueTimeout());
    }



    private static boolean canModifyQueueProp(ThreadPoolProperties properties) {
        return Objects.equals(properties.getQueueType(), QueueTypeEnum.VARIABLE_LINKED_BLOCKING_QUEUE.getName());
    }

}
