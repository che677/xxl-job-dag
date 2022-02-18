package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.dao.HopMapper;
import com.xxl.job.admin.service.HopService;
import com.xxl.job.core.biz.model.HopEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class HopServiceImpl implements HopService {

    @Autowired
    HopMapper hopMapper;

    @Override
    public Integer addHop(List<HopEntity> hopEntityList){
        return hopMapper.insertBatch(hopEntityList);
    }

    @Override
    public Integer updateHop(HopEntity hopEntity){
        return hopMapper.updateByPrimaryKey(hopEntity);
    }

    @Override
    public Integer deleteBatch(List<Integer> ids){
        return hopMapper.deleteBatch(ids);
    }

//    @LogAnno(operateType = "查询连线顺序")
    @Override
    public List<HopEntity> queryByFlowId(Integer flowId){
        return hopMapper.queryByFlowId(flowId);
    }
    
}
