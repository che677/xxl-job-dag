package com.xxl.job.workbench.controller;

import com.xxl.job.core.biz.model.MqEntity;
import com.xxl.job.workbench.dao.MqMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@RestController
@Slf4j
public class KafkaController {

    @Autowired
    private KafkaTemplate<String, String> template;

    @Autowired
    private MqMapper mqMapper;

    @PostMapping("/send/{message}")
    public void send(@PathVariable String message) {
        final ListenableFuture<SendResult<String, String>> future =
                template.send("tp_demo_01", message);
        try {
            SendResult result = future.get();
            RecordMetadata recordMetadata = result.getRecordMetadata();
            System.out.println(recordMetadata.topic() + "\t" + recordMetadata.partition());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/sendAsync/{message}")
    public void sendAsync(@PathVariable String message) {
        final ListenableFuture<SendResult<String, String>> future =
                template.send("topic-spring-01", 0, "111", message);
        // 这种是异步发送的，发送结果是异步获取的
        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onFailure(Throwable throwable) {
                System.out.println("发送失败");
            }

            @Override
            public void onSuccess(SendResult<String, String> result) {
                RecordMetadata recordMetadata = result.getRecordMetadata();
                System.out.println("发送成功" + recordMetadata.topic() + "\t" + recordMetadata.partition());
            }
        });
    }

    @PostMapping("/consume")
    public void consume() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("bootstrap.servers", "master:9092");
        configs.put("group.id", "mygrp");
        // 设置偏移量自动提交。自动提交是默认值。这里做示例。
        configs.put("enable.auto.commit", "true");
        // 偏移量自动提交的时间间隔
        configs.put("auto.commit.interval.ms", "3000");
        configs.put("key.deserializer", StringDeserializer.class);
        configs.put("value.deserializer", StringDeserializer.class);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(configs);
        consumer.subscribe(Collections.singleton("lose_topic"));
        try {
            int i = 0;
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                records.iterator().forEachRemaining(r -> System.out.println(r.key() + "\t" + r.value()));
                // 异步提交offset
                consumer.commitAsync((offsets, exception) -> {
                    if (exception != null) {
                        exception.printStackTrace();
                    }
                });
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                consumer.commitSync();
                // 最后一次提交使用同步阻塞式提交,因为异步提交没有重试机制，不知道提交是否成功
            } finally {
                consumer.close();
            }
        }
    }

    @PostMapping("/testInterceptor")
    public void testInterceptor(){
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "master:9092");
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 如果有多个拦截器，则设置为多个拦截器类的全限定类名，中间用逗号隔开
        configs.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, "com.xxl.job.workbench.config.MyInterceptor");
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(configs);
        ProducerRecord<String, String> record = new ProducerRecord<>(
                "tp_inter_01",
            0,
            "1001",
            "lagou message"
                );
        producer.send(record, new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                if(exception == null){
                    System.out.println(metadata.offset());
                }
            }
        });
        producer.close();
    }

    @PostMapping("/offset")
    public void offset(){
        String topic = "tp_demo_01";
        Map<String, Object> configs = new HashMap<>();
        configs.put("bootstrap.servers", "master:9092");
        // 消费者在消费主题时，一个消费者属于一个消费组；
        configs.put("group.id", "mygrp1");
        configs.put("key.deserializer", StringDeserializer.class);
        configs.put("value.deserializer", StringDeserializer.class);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(configs);
//        consumer.subscribe(Collections.singleton("tp_demo_01"));

        // 如何手动给消费者分配分区？
        // 1、需要知道有哪些主题可以访问，和消费(一个消费者可以访问多个topic，一个topic也可以被多个消费者访问)
        Map<String, List<PartitionInfo>> stringListMap = consumer.listTopics();
        stringListMap.forEach(new BiConsumer<String, List<PartitionInfo>>() {
            @Override
            public void accept(String s, List<PartitionInfo> partitionInfos) {
                System.out.println("主题名称: "+s);
                for (PartitionInfo p: partitionInfos) {
                    System.out.println(p);
                }
            }
        });

//        Set<TopicPartition> assignment = consumer.assignment();
//        assignment.forEach(r-> System.out.println(r));
//        // 2、给当前消费者分配指定的主题分区
//        consumer.assign(Arrays.asList(
//                new TopicPartition("tp_demo_01", 0),
//                new TopicPartition("tp_demo_01", 1),
//                new TopicPartition("tp_demo_01", 2)
//        ));
//        Set<TopicPartition> assignment1 = consumer.assignment();
//        assignment1.forEach(r-> System.out.println(r));

        // 看一下消费者在主题的分区上的消费者偏移量
//        long tp_demo_01 = consumer.position(new TopicPartition(topic, 0));
//        System.out.println(tp_demo_01);
        // 指定分区的偏移量为最开始（否则是从最新，也就是从最新发送过来的消息中消费）
        consumer.subscribe(Collections.singleton("tp_demo_01"));
//        consumer.seekToBeginning(Arrays.asList(new TopicPartition(topic, 0),
//                new TopicPartition(topic, 2)));
        int i = 0;
        while(i++<100){
            ConsumerRecords<String, String> records = consumer.poll(1_000);
            records.forEach(new Consumer<ConsumerRecord<String, String>>() {
                @Override
                public void accept(ConsumerRecord<String, String> stringStringConsumerRecord) {
                    System.out.println(records);
                }
            });
        }
        consumer.close();
    }

    @PostMapping("/kafkaTs")
    public void kafkaTs(){
        Map<String, Object> configs = new HashMap<>();
        configs.put("bootstrap.servers", "master:9092");
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 提供生产者client.id
        configs.put(ProducerConfig.CLIENT_ID_CONFIG, "tx_producer");
        // 设置事务id
        configs.put("transactional.id", "my_tx_id_1");
        // 需要isr全体确认消息
        configs.put("acks", "all");
        KafkaProducer<String,String> producer = new KafkaProducer<String, String>(configs);
        // 初始化事务
        producer.initTransactions();
        try{
            // 开启事务
            producer.beginTransaction();
            // 发送事务消息
            producer.send(new ProducerRecord<>("tp_tx_01", "txkey4", "tx_msg_4"));
            producer.send(new ProducerRecord<>("tp_tx_01", "txkey5", "tx_msg_5"));
            producer.send(new ProducerRecord<>("tp_tx_01", "txkey6", "tx_msg_6"));
            int i = 1/0;
            // 提交事务
            producer.commitTransaction();
        }catch (Exception e){
            // 事务回滚
            producer.abortTransaction();
        }finally {
            // 关闭生产者
            producer.close();
        }

    }

    @PostMapping("/commitOffset")
    public void commitOffset() {
        String topic = "tp_demo_01";
        Map<String, Object> configs = new HashMap<>();
        configs.put("bootstrap.servers", "master:9092");
        // 消费者在消费主题时，一个消费者属于一个消费组；
        configs.put("group.id", "mygrp1");
        configs.put("key.deserializer", StringDeserializer.class);
        configs.put("value.deserializer", StringDeserializer.class);
        // 手动提交
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, false);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(configs);
        consumer.subscribe(Collections.singleton(topic));
        while(true){
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<String, String> consumerRecord:records){
                System.out.println(consumerRecord);
            }
            // 同步提交offset
            consumer.commitSync();
            // 异步提交offset
            consumer.commitAsync();
        }
    }

    @PostMapping("/setOffset")
    public void setOffset() {
        String topic = "tp_demo_01";
        Map<String, Object> configs = new HashMap<>();
        configs.put("bootstrap.servers", "master:9092");
        // 消费者在消费主题时，一个消费者属于一个消费组；
        configs.put("group.id", "mygrp1");
        configs.put("key.deserializer", StringDeserializer.class);
        configs.put("value.deserializer", StringDeserializer.class);
        // 手动提交
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, false);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(configs);
        consumer.subscribe(Collections.singleton(topic));

        // 获取主题分区
        Set<TopicPartition> assignment = consumer.assignment();
        // 保证分区方案已经指定完毕，这里是利用自旋的方式来不断获取分区信息，直到拿到真实信息
        while(assignment.size() == 0){
            consumer.poll(Duration.ofSeconds(1));
            assignment = consumer.assignment();
        }
        // 指定分区的offset进行消费
        for (TopicPartition topicPartition:assignment){
            consumer.seek(topicPartition, 100);
        }

        // 指定时间
        HashMap<TopicPartition, Long> topicPartitionLongHashMap = new HashMap<>();
        // 封装集合，获取一天前的offset位置
        for (TopicPartition topicPartition : assignment) {
            topicPartitionLongHashMap.put(topicPartition, System.currentTimeMillis()-1*24*3600*1000);
        }
        Map<TopicPartition, OffsetAndTimestamp> map = consumer.offsetsForTimes(topicPartitionLongHashMap);
        // 指定该时间的offset
        for (TopicPartition topicPartition : assignment) {
            OffsetAndTimestamp offsetAndTimestamp = map.get(topicPartition);
            consumer.seek(topicPartition, offsetAndTimestamp.offset());
        }


        // 进行消费
        while(true){
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<String, String> consumerRecord:records){
                System.out.println(consumerRecord);
            }
            // 异步提交offset
            consumer.commitAsync();
        }
    }


    @PostMapping("/loseSend")
    public void loseSend(Integer epoch){
        String topic = "lose_topic";
        for(int i = 0; i<epoch; i++){
            String key = String.valueOf(i);
            try{
                final ListenableFuture<SendResult<String, String>> future =
                        template.send(topic, key, "ddd");
                // 这种是异步发送的，发送结果是异步获取的，当ack实现之后才会返回callback函数
                future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        log.error("网络超时发送失败");
                        MqEntity entity = new MqEntity();
                        entity.setMessageId(key);
                        entity.setContent(key);
                        entity.setMessageStatus(0);
                        try{
                            int insert = mqMapper.insert(entity);
                        }catch (Exception e){
                            log.error("插入表失败:    ", key);
                        }
                    }

                    @Override
                    public void onSuccess(SendResult<String, String> result) {
                        RecordMetadata recordMetadata = result.getRecordMetadata();
                        System.out.println("发送成功" + recordMetadata.topic() + "\t" + recordMetadata.partition());
                    }
                });
            }catch (Exception e){
                log.error("任务报错发送失败");
                MqEntity entity = new MqEntity();
                entity.setMessageId(key);
                entity.setContent(key);
                entity.setMessageStatus(0);
                try{
                    int insert = mqMapper.insert(entity);
                }catch (Exception e1){
                    log.error("插入表失败:    ", key);
                }
            }
        }
    }

    @PostMapping("/loseConsume")
    public void loseConsume(){
        String topic = "lose_topic";
        Map<String, Object> configs = new HashMap<>();
        configs.put("bootstrap.servers", "master:9092");
        // 消费者在消费主题时，一个消费者属于一个消费组；
        configs.put("group.id", "mygrp1");
        configs.put("key.deserializer", StringDeserializer.class);
        configs.put("value.deserializer", StringDeserializer.class);
        // 手动提交
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, false);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(configs);
        consumer.subscribe(Collections.singleton(topic));
    }

}
