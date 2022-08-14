package com.xxl.job.workbench.config;

import com.xxl.job.dp.common.property.DtpProperties;
import com.xxl.job.dp.common.property.ThreadPoolProperties;
import com.xxl.job.dp.common.refresher.ZookeeperRefresher;
import com.xxl.job.dp.common.utils.ExecutorConverter;
import com.xxl.job.dp.core.DtpMonitor;
import com.xxl.job.dp.core.DtpRegistry;
import com.xxl.job.workbench.property.DynamicProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@Slf4j
public class DpConfig {

    @Autowired
    DynamicProperties properties;

    /**
     * 根据properties文件，注册线程池
     */
    @Bean
    public ZookeeperRefresher initDp(){
        // 1、先通过配置中心拿到executor的配置信息，同时注册线程池
        // 2、注册监听器，监控zk的状态
        DtpProperties dtpProperties = new DtpProperties();
        BeanUtils.copyProperties(properties, dtpProperties);
        BeanUtils.copyProperties(properties.getZookeeper(), dtpProperties.getZookeeper());
        dtpProperties.setRejectAlarm(true);
        List<ThreadPoolProperties> executors = properties.getExecutors();
        DtpRegistry.setDtpProperties(dtpProperties);
        for(ThreadPoolProperties properties:executors){
            DtpRegistry.registerDtp(ExecutorConverter.revert(properties));
        }
        System.out.println("一共启动了如下线程池:  " + DtpRegistry.listAllDtpNames());
        ZookeeperRefresher zk = new ZookeeperRefresher();
        DtpMonitor.getInstance().start();
        return zk;
    }

}
