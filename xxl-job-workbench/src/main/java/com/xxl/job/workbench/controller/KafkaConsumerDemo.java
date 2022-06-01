package com.xxl.job.workbench.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class KafkaConsumerDemo {

    /**
     * MANUAL_IMMEDIATE 手动调用Acknowledgment.acknowledge()后立即提交
     * @param record
     */
    @KafkaListener(topics = "lose_topic")
    public void onMessageManualImmediate(ConsumerRecord<String,String> record, Acknowledgment ack){
        System.out.println(record);
        ack.acknowledge();//直接提交offset
    }

}
