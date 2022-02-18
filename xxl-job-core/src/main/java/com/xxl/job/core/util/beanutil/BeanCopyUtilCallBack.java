package com.xxl.job.core.util.beanutil;

@FunctionalInterface
public interface BeanCopyUtilCallBack <S, T>{
    void callback(S s, T t);
}
