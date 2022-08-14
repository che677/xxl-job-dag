package com.xxl.job.dp.core;

import cn.hutool.core.util.NumberUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.xxl.job.dp.common.cons.DynamicTpConst;
import com.xxl.job.dp.common.enumerate.NotifyTypeEnum;
import com.xxl.job.dp.common.refresher.ZookeeperRefresher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.*;

/**
 * 线程池监控类，每隔
 */

@Slf4j
public class DtpMonitor  {

    private volatile static DtpMonitor dtpMonitor;

    public static DtpMonitor getInstance(){
        if(dtpMonitor==null){
            synchronized (DtpMonitor.class){
                if(dtpMonitor==null){
                    dtpMonitor = new DtpMonitor();
                }
                return dtpMonitor;
            }
        }else{
            return dtpMonitor;
        }
    }

    private static final ScheduledExecutorService MONITOR_EXECUTOR = new ScheduledThreadPoolExecutor(
            1, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("DtpMonitor").build());

    public void start(){
        MONITOR_EXECUTOR.scheduleWithFixedDelay(this::alarm,
                0, DtpRegistry.getDtpProperties().getMonitorInterval(), TimeUnit.DAYS);
    }

    private void alarm(){
        List<String> dtpNames = DtpRegistry.listAllDtpNames();
        // 针对每一个线程池进行监控
        dtpNames.forEach(x -> {
            DtpExecutor executor = DtpRegistry.getDtpExecutor(x);
            doAlarm(executor, DynamicTpConst.SCHEDULE_ALARM_TYPES);
        });
    }

    // 针对每一种类型进行监控
    public static void doAlarm(DtpExecutor executor, List<NotifyTypeEnum> typeEnums) {
        typeEnums.forEach(x -> {
            if(checkThreshold(executor, x)){
                log.warn("线程池超过临界值: {}", executor.getThreadPoolName(),
                        ", 需要降低", x.getValue());
            }
        });
    }

    private static boolean checkThreshold(DtpExecutor executor, NotifyTypeEnum notifyType) {
        switch (notifyType) {
            case CAPACITY:
                return checkCapacity(executor);
            case LIVENESS:
                return checkLiveness(executor);
                // Reject不用写，因为case when如果不用break，就会直接跳到default中执行
            case REJECT:
                return true;
            default:
                log.error("Unsupported alarm type, type: {}", notifyType);
                return false;
        }
    }

    private static boolean checkLiveness(DtpExecutor executor) {
        int maximumPoolSize = executor.getMaximumPoolSize();
        double div = NumberUtil.div(executor.getActiveCount(), maximumPoolSize, 2) * 100;
        return div >= DtpRegistry.getDtpProperties().getThreshold();
    }

    private static boolean checkCapacity(DtpExecutor executor) {
        BlockingQueue<Runnable> workQueue = executor.getQueue();
        if (CollectionUtils.isEmpty(workQueue)) {
            return false;
        }
        int queueCapacity = executor.getQueue().size() + executor.getQueue().remainingCapacity();
        double div = NumberUtil.div(workQueue.size(), queueCapacity, 2) * 100;
        return div >= DtpRegistry.getDtpProperties().getThreshold();
    }

}
