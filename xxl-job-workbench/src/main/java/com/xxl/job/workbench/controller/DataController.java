package com.xxl.job.workbench.controller;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.workbench.dao.OrderEntityMapper;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataController {

    @Autowired
    private OrderEntityMapper mapper;
    // 1、百万级Excel插入怎么处理
    // 2、高并发下写数据，如何快速响应  线程池非常容易打满
    // 3、大批量消费mq，如何提升数据处理效率
    @PostMapping("/saveBatch")
    public ReturnT<String> save(){

        return ReturnT.SUCCESS;
    }

}
