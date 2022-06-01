package com.xxl.job.workbench.config;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;

public class MyInterceptor implements ProducerInterceptor {


    @Override
    public ProducerRecord onSend(ProducerRecord record) {
        ProducerRecord newRecord = new ProducerRecord(record.topic(), record.value()+"_xxxxxxx");
        System.out.println("newRecord");
        return newRecord;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {

    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {
        Object classContent = configs.get("classContent");
        System.out.println(classContent);
    }
}
