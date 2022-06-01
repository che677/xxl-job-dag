package com.xxl.job.workbench.config;

import com.xxl.job.core.biz.model.DelayElement;
import lombok.SneakyThrows;
import org.redisson.Redisson;
import org.redisson.RedissonAtomicLong;
import org.redisson.api.RAtomicLong;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.PostConstruct;
import java.util.UUID;
import java.util.concurrent.*;

@Configuration
public class DelayConfig{

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private Redisson redisson;

    private static DelayConfig config = new DelayConfig();
    private static DelayQueue delay = new DelayQueue();
    private static boolean status = true;
    private Thread consumer;
    private RAtomicLong count = null;

    // 这里也可以直接打一个bean注解，以后引用都使用autowire的方式注入
    @PostConstruct
    public void initMethod(){
        consumer = new Thread(){
            @SneakyThrows
            @Override
            public void run() {
                while(status){
                    DelayElement take = (DelayElement)delay.take();
                    redisTemplate.delete("catalogJson");
                    System.out.println("===================================================="+take.getTimestamp());
                }
            }
        };
        consumer.setDaemon(true);
        consumer.start();
        count = redisson.getAtomicLong("testlong");
        count.set(10000);
    }

    public static DelayConfig getInstance(){
        return config;
    }

    public void addTask(DelayElement element) throws InterruptedException {
        delay.put(element);
    }

    public void shutdown(){
        status = false;
        consumer.interrupt();
    }

    public RAtomicLong getCount(){
        return count;
    }

}
