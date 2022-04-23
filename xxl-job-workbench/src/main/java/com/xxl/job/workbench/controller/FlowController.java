package com.xxl.job.workbench.controller;

import com.alibaba.fastjson.JSON;
import com.xxl.job.core.biz.model.Catelog2Vo;
import com.xxl.job.core.biz.model.FlowEntity;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.workbench.service.CategoryService;
import com.xxl.job.workbench.service.FlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
public class FlowController {

    @Autowired
    private FlowService flowService;

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

}
