//package com.xxl.job.workbench.config;
//
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.common.serialization.StringDeserializer;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
//import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.annotation.EnableKafka;
//import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
//import org.springframework.kafka.config.KafkaListenerContainerFactory;
//import org.springframework.kafka.core.ConsumerFactory;
//import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
//import org.springframework.kafka.listener.AbstractMessageListenerContainer;
//import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
//import org.springframework.kafka.listener.ContainerProperties;
//import org.springframework.kafka.support.converter.*;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//@EnableKafka
//public class KafkaConsumerConfig {
//
//    private final Logger logger = LoggerFactory.getLogger(this.getClass());
//    @Value("${spring.kafka.bootstrap-servers}")
//    private String servers;
//    @Value("${spring.kafka.consumer.enable-auto-commit}")
//    private boolean enableAutoCommit;
//    @Value("${spring.kafka.consumer.auto-commit-interval}")
//    private String autoCommitInterval;
//    @Value("${spring.kafka.consumer.group-id}")
//    private String groupId;
//    @Value("${spring.kafka.consumer.auto-offset-reset}")
//    private String autoOffsetReset;
//    @Value("${spring.kafka.consumer.concurrency}")
//    private int concurrency;
//    @Value("${spring.kafka.consumer.maxpollrecordsconfig}")
//    private int maxPollRecordsConfig;
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
//        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(consumerFactory());
//        factory.setConcurrency(concurrency); // 监听器线程数
//        factory.getContainerProperties().setPollTimeout(1500);
//        factory.setBatchListener(true);
//        factory.setMessageConverter(new String());
//        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
//        return factory;
//    }
//
//    public ConsumerFactory<String, String> consumerFactory() {
//        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
//    }
//
//    public Map<String, Object> consumerConfigs() {
//        Map<String, Object> propsMap = new HashMap<>(9);
//        propsMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
//        propsMap.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
//        propsMap.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, autoCommitInterval);
////        propsMap.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeout); // 会话响应时间，超时了kafka就会放弃消费，或者消费下一条数据
//        propsMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        propsMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        propsMap.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
//        propsMap.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
//        propsMap.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecordsConfig);//每个批次获取数据的最大值
//        return propsMap;
//    }
//
//}
