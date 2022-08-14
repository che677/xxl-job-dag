package com.xxl.job.dp.common.reject;

import com.google.common.collect.Lists;
import com.xxl.job.dp.common.enumerate.NotifyTypeEnum;
import com.xxl.job.dp.core.DtpExecutor;
import com.xxl.job.dp.core.DtpMonitor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadPoolExecutor;

public class RejectedInvacationHandler implements InvocationHandler {

    private final Object target;

    private boolean enable;

    public RejectedInvacationHandler(Object target, boolean enable) {
        this.target = target;
        this.enable = enable;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            // 根据参数，执行
            ThreadPoolExecutor executor = (ThreadPoolExecutor) args[1];
            if (executor instanceof DtpExecutor) {
                DtpExecutor dtpExecutor = (DtpExecutor) executor;
                // 提高线程池的拒绝数量
                dtpExecutor.incRejectCount(1);
                if(enable){
                    // 如果启动了监控，就直接开启reject预警
                    DtpMonitor.doAlarm(dtpExecutor, Lists.newArrayList(NotifyTypeEnum.REJECT));
                }
            }
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }
}
