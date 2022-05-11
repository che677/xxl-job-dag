package com.xxl.job.workbench.annotation;

import com.xxl.job.workbench.config.DatabaseType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义数据源注解
 *
 * @author: mason
 * @since: 2020/1/9
 **/
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DS {
    /**
     * 数据源名称
     * @return
     */
    DatabaseType value() default DatabaseType.master;
}
