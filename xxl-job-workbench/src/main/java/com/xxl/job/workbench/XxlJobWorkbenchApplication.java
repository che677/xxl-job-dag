package com.xxl.job.workbench;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;

@EnableDubbo
@SpringBootApplication
public class XxlJobWorkbenchApplication {

    public static void main(String[] args) {
        SpringApplication.run(XxlJobWorkbenchApplication.class, args);
    }

}
