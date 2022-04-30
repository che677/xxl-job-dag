package com.xxl.job.workbench.controller;

import com.alibaba.fastjson.JSON;
import com.xxl.job.core.biz.model.Catelog2Vo;
import com.xxl.job.core.biz.model.FlowEntity;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.dubbo.AccountService;
import com.xxl.job.core.dubbo.OrderService;
import com.xxl.job.workbench.service.CategoryService;
import com.xxl.job.workbench.service.FlowService;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
public class FlowController {

    @Autowired
    private FlowService flowService;

    @Reference(check = false)
    private AccountService accountService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/getflow")
    public ReturnT<String> chartInfo(Integer flowId) {
        FlowEntity flowEntity = flowService.executeFlow(flowId);
        ReturnT<String> success = ReturnT.SUCCESS;
        success.setContent(JSON.toJSONString(flowEntity));
        return success;
    }

    @GetMapping("/getRedis")
    public String testRedis(){
        return categoryService.testRedis();
    }

    @GetMapping("/catalog")
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        Map<String, List<Catelog2Vo>> catalogJson = categoryService.getCatalogJson();
        return catalogJson;
    }

    @PostMapping("/updateOrder")
    public ReturnT<String> updateOrder(){
        accountService.saveOrUpdateAccount();
        orderService.saveOrder();
        return ReturnT.SUCCESS;
    }

}
