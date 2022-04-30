package com.xxl.job.workbench.controller;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
public class KafkaController {

    @Autowired
    private KafkaTemplate<Integer, String> template;

    @PostMapping("/send/{message}")
    public void send(@PathVariable String message) {
        final ListenableFuture<SendResult<Integer, String>> future =
                template.send("topic-spring-01", 0, 0, message);
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
        final ListenableFuture<SendResult<Integer, String>> future =
                template.send("topic-spring-01", 0, 1, message);
        // 这种是异步发送的，发送结果是异步获取的
        future.addCallback(new ListenableFutureCallback<SendResult<Integer, String>>() {
            @Override
            public void onFailure(Throwable throwable) {
                System.out.println("发送失败");
            }

            @Override
            public void onSuccess(SendResult<Integer, String> result) {
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
        consumer.subscribe(Collections.singleton("tp_demo_01"));
        try {
            int i = 0;
            while (i++<100) {
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

}
