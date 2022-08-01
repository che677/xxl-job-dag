package com.xxl.job.workbench.config;

import com.xxl.job.delay.core.RedisDelayQueueContext;
import com.xxl.job.delay.iface.RedisDelayQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisDelayConfig {

    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate redisTemplate;

    /**修改 redisTemplate 的key序列化方式  **/
//    @Autowired(required = false)
//    public void setRedisTemplate(RedisTemplate redisTemplate) {
//        RedisSerializer stringSerializer = new StringRedisSerializer();
//        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
//        redisTemplate.setKeySerializer(stringSerializer);
//        redisTemplate.setHashKeySerializer(stringSerializer);
//        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
//        this.redisTemplate = redisTemplate;
//    }

    /******* 接入 RedisDelayQueue  *******/
    @Bean
    public RedisDelayQueueContext getRdctx(){
        RedisDelayQueueContext context =  new RedisDelayQueueContext(redisTemplate);
        return context;
    }

    @Bean
    public RedisDelayQueue getRedisOperation(RedisDelayQueueContext context){
        return context.getRedisDelayQueue();
    }


}
