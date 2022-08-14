package com.xxl.job.workbench.property;

import com.xxl.job.dp.common.property.DtpProperties;
import com.xxl.job.dp.common.property.ThreadPoolProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Main properties that maintain by config center.
 *
 * @author: yanhom
 * @since 1.0.0
 **/
@Data
@Component
@ConfigurationProperties(prefix = "spring.dynamic.tp")
public class DynamicProperties {

    private boolean isRejectAlarm;

    private int threshold;

    /**
     * If enabled DynamicTp.
     */
    private boolean enabled = true;

    /**
     * If print banner.
     */
    private boolean enabledBanner = true;


    /**
     * Zookeeper config.
     */
    private DtpProperties.Zookeeper zookeeper;

    /**
     * Config file type.
     */
    private String configType = "yml";

    /**
     * If enabled metrics collect.
     */
    private boolean enabledCollect = false;

    /**
     * Metrics collector type.
     */
    public String collectorType = "logging";

    /**
     * MetricsLog storage path
     */
    public String logPath;

    /**
     * Monitor interval, time unit（s）
     */
    private int monitorInterval = 5;

    /**
     * ThreadPoolExecutor configs.
     */
    private List<ThreadPoolProperties> executors;

}
