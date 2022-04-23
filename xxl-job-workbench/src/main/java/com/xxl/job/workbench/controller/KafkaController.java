package com.xxl.job.workbench.controller;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KafkaController {

    @Autowired
    private KafkaTemplate<Integer, String> template;

    @PostMapping("/send/{message}")
    public void send(@PathVariable String message){
        final ListenableFuture<SendResult<Integer, String>> future =
                template.send("topic-spring-01", 0,0,message);
        try{
            SendResult result = future.get();
            RecordMetadata recordMetadata = result.getRecordMetadata();
            System.out.println(recordMetadata.topic()+"\t"+recordMetadata.partition());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @PostMapping("/sendAsync/{message}")
    public void sendAsync(@PathVariable String message){
        final ListenableFuture<SendResult<Integer, String>> future =
                template.send("topic-spring-01", 0,1,message);
        // 这种是异步发送的，发送结果是异步获取的
        future.addCallback(new ListenableFutureCallback<SendResult<Integer, String>>() {
            @Override
            public void onFailure(Throwable throwable) {
                System.out.println("发送失败");
            }

            @Override
            public void onSuccess(SendResult<Integer, String> result) {
                RecordMetadata recordMetadata = result.getRecordMetadata();
                System.out.println("发送成功"+recordMetadata.topic()+"\t"+recordMetadata.partition());
            }
        });
    }

}
