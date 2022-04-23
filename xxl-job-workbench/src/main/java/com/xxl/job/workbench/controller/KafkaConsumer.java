package com.xxl.job.workbench.controller;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {

    @KafkaListener(topics = "topic-spring-01")
    public void onMessage(ConsumerRecord<Integer,String> record){
        System.out.println("消费成功"+record.key()+"\t"+record.value());
    }

}
