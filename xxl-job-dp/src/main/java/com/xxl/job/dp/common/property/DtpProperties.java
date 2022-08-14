package com.xxl.job.dp.common.property;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

/**
 * Main properties that maintain by config center.
 *
 * @author: yanhom
 * @since 1.0.0
 **/
@Slf4j
@Data
public class DtpProperties {

    private boolean rejectAlarm;

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
    private Zookeeper zookeeper;

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

    @Data
    public static class Zookeeper {

        private String zkConnectStr;

        private String configVersion;

        private String rootNode;

        private String node;

        private String configKey;
    }
}
