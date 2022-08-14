package com.xxl.job.workbench;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

import java.util.ArrayList;
import java.util.List;

@EnableConfigurationProperties
@EnableDubbo
@SpringBootApplication
//@EnableCaching
public class XxlJobWorkbenchApplication {

    public static void main(String[] args) {
        SpringApplication.run(XxlJobWorkbenchApplication.class, args);
    }

}
