package com.xxl.job.workbench.service.impl;

import com.xxl.job.core.biz.model.FlowEntity;
import com.xxl.job.workbench.dao.FlowMapper;
import com.xxl.job.workbench.service.FlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlowServiceImpl implements FlowService {

    @Autowired
    private FlowMapper flowMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public FlowEntity executeFlow(int flowId){
        FlowEntity flowEntities = flowMapper.selectByPrimaryKey(flowId);
        return flowEntities;
    }


}
