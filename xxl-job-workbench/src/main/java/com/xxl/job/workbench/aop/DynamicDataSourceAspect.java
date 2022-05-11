package com.xxl.job.workbench.aop;

import com.xxl.job.workbench.annotation.DS;
import com.xxl.job.workbench.config.DatabaseContextHolder;
import com.xxl.job.workbench.config.DatabaseType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * TODO
 *
 * @author: mason
 * @since: 2020/1/9
 **/
@Aspect
@Component
public class DynamicDataSourceAspect {

    @Pointcut("@annotation(com.xxl.job.workbench.annotation.DS)")
    public void dataSourcePointCut(){
    }

    @Around("dataSourcePointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        DatabaseType dsKey = getDSAnnotation(joinPoint).value();
        DatabaseContextHolder.setDatabaseType(dsKey);
        try{
            return joinPoint.proceed();
        }finally {
            DatabaseContextHolder.removeDatabaseType();
        }
    }

    /**
     * 根据类或方法获取数据源注解
     * @param joinPoint
     * @return
     */
    private DS getDSAnnotation(ProceedingJoinPoint joinPoint){
        Class<?> targetClass = joinPoint.getTarget().getClass();
        DS dsAnnotation = targetClass.getAnnotation(DS.class);
        // 先判断类的注解，再判断方法注解
        if(Objects.nonNull(dsAnnotation)){
            return dsAnnotation;
        }else{
            MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
            return methodSignature.getMethod().getAnnotation(DS.class);
        }
    }
}