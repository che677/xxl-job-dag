package com.xxl.job.core.biz.model;

import lombok.Data;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
public class DelayElement implements Delayed{
    private final long timestamp;

    private final String name;

    public DelayElement(String name, long timestamp, TimeUnit unit) {
        this.timestamp = System.currentTimeMillis() + (timestamp > 0 ? unit.toMillis(timestamp) : 0);
        this.name = name;
    }

    /**
     * 返回剩余的延迟
     * poll方法根据该方法判断，只有延迟<0才出队
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return timestamp - System.currentTimeMillis();
    }

    /**
     * 决定堆排序
     */
    @Override
    public int compareTo(Delayed o) {
        DelayElement delayElement = (DelayElement) o;
        long diff = this.timestamp - delayElement.timestamp;
        if (diff <= 0) {
            return -1;
        } else {
            return 1;
        }
    }
}
