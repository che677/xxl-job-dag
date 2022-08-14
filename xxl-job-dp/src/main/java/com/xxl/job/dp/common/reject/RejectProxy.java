package com.xxl.job.dp.common.reject;

import com.xxl.job.dp.common.enumerate.RejectedTypeEnum;

import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class RejectProxy {

    public static RejectedExecutionHandler getReject(String name){
        if (Objects.equals(name, RejectedTypeEnum.ABORT_POLICY.getName())) {
            return new ThreadPoolExecutor.AbortPolicy();
        } else if (Objects.equals(name, RejectedTypeEnum.CALLER_RUNS_POLICY.getName())) {
            return new ThreadPoolExecutor.CallerRunsPolicy();
        } else if (Objects.equals(name, RejectedTypeEnum.DISCARD_OLDEST_POLICY.getName())) {
            return new ThreadPoolExecutor.DiscardOldestPolicy();
        } else if (Objects.equals(name, RejectedTypeEnum.DISCARD_POLICY.getName())) {
            return new ThreadPoolExecutor.DiscardPolicy();
        }else {
            return null;
        }
    }

    public static RejectedExecutionHandler getProxy(String name, boolean enable) {
        RejectedExecutionHandler handler = getReject(name);
        return (RejectedExecutionHandler) Proxy
                .newProxyInstance(handler.getClass().getClassLoader(),
                        new Class[]{RejectedExecutionHandler.class},
                        new RejectedInvacationHandler(handler, enable));
    }

    public static RejectedExecutionHandler getProxy(RejectedExecutionHandler handler, boolean enable) {
        return (RejectedExecutionHandler) Proxy
                .newProxyInstance(handler.getClass().getClassLoader(),
                        new Class[]{RejectedExecutionHandler.class},
                        new RejectedInvacationHandler(handler, enable));
    }

}
