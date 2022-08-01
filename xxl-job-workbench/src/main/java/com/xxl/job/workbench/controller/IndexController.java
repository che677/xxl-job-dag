package com.xxl.job.workbench.controller;

import com.xxl.job.delay.common.Args;
import com.xxl.job.delay.core.RedisDelayQueueContext;
import com.xxl.job.delay.iface.RedisDelayQueue;
import com.xxl.job.workbench.delayqueue.DelayQueueCallBackDemo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

/**
 * @Description TODO
 * @Author shirenchuang
 * @Date 2019/8/1 9:40 AM
 **/
@RestController
public class IndexController {

    @Autowired
    RedisDelayQueue redisDelayQueue;

    @Autowired
    RedisDelayQueueContext redisDelayQueueContext;

    @Autowired
    DelayQueueCallBackDemo delayQueueDemo2;

    @PostMapping("/addJob")
    public void addJob(Long rt){
        if(rt ==null){
            rt = System.currentTimeMillis()+5000;
        }
        Args myArgs = new Args();
        String id = UUID.randomUUID().toString();
        myArgs.setId(id);
        redisDelayQueue.add(myArgs,"workbench",rt);
    }

    @PostMapping("/delJob2")
    public void delJob2(String userId ){
        redisDelayQueue.delete("workbench", userId);
    }

    private Date getDate(long millis){
        Date date = new Date();
        date.setTime(millis);
        return date;
    }

}
