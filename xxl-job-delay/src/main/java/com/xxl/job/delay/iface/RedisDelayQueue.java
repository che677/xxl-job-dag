package com.xxl.job.delay.iface;

import com.xxl.job.delay.common.Args;


/**
 * @Description 提供给客户端使用的 延迟队列操作
 * @Author shirenchuang
 * @Date 2019/7/30 4:58 PM
 **/
public interface RedisDelayQueue {

    /**
     * 新增一个延迟任务
     * @param args  用户入参
     * @param topic
     * @param runTimeMillis  执行时间 单位: 毫秒
     */
    public void add(Args args, String topic, long runTimeMillis);

    /**
     * 新增一个延迟任务
     * @param args
     * @param delayTimeMillis   需要延迟的时间:  单位: 毫秒
     * @param topic
     */
    public void add(Args args, long delayTimeMillis, String topic);

    /**
     * 删除一个延迟队列
     * @param topic
     * @param id
     */
    public void delete(String topic, String id);

}
