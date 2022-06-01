package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.thread.JobTriggerPoolHelper;
import com.xxl.job.admin.dao.MqMapper;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.model.MqEntity;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Slf4j
@Component
public class KafkaConsumer {

    @Autowired
    private XxlJobService xxlJobService;
    @Autowired
    private MqMapper mqMapper;

    @KafkaListener(topics = "collect_data")
    public void dataCollect(ConsumerRecord<String,String> record, Acknowledgment ack){
        // 幂等性考虑，先查询xxljob有没有数据库，再执行insert操作
        String key = record.key();
        String value = record.value();
        System.out.println(key+":       "+value);
        try{
            XxlJobInfo xxlJobInfo = xxlJobService.getByUnique(key);
            ReturnT<String> res = null;
            // 如果是空的，就新建任务，并触发执行
            if(ObjectUtils.isEmpty(xxlJobInfo)){
                xxlJobInfo = new XxlJobInfo();
                xxlJobInfo.setExecutorBlockStrategy(ExecutorBlockStrategyEnum.SERIAL_EXECUTION.toString());
                xxlJobInfo.setAddTime(new Date());
                xxlJobInfo.setExecutorHandler("ttttt");
                xxlJobInfo.setFlowId(Integer.valueOf(key));
                xxlJobInfo.setJobDesc("测试任务");
                xxlJobInfo.setJobGroup(1);
                xxlJobInfo.setExecutorRouteStrategy(ExecutorRouteStrategyEnum.FIRST.toString());
                xxlJobInfo.setAuthor("admin");
                res = xxlJobService.add(xxlJobInfo);
                // 启动任务
                // xxlJobService.start(id);
                // 触发任务
                // JobTriggerPoolHelper.trigger();
                System.out.println("新建后台元数据采集任务");
                if(null != res && res.getCode() != 200){
                    System.out.println("新增元数据采集任务失败"+res.getMsg());
                    throw new Exception();
                }
            }else{
                // 如果已经存在任务了，就不处理，保证消息的幂等性
            }
        }catch (Exception e){
            // 消费失败，直接丢入mq本地消息表，然后启动后台xxljob任务，定期执行那些尚未被处理的消息
            log.error("任务报错发送失败");
            MqEntity entity = new MqEntity();
            entity.setMessageId(key);
            entity.setContent(key);
            entity.setMessageStatus(0);
            mqMapper.insert(entity);
        }
        // 处理完成之后再提交offset，这样可以防止消息丢失
        ack.acknowledge();
    }

}
