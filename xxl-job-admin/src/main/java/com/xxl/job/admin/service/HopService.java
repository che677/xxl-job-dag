package com.xxl.job.admin.service;

import com.xxl.job.core.biz.model.HopEntity;

import java.util.List;

public interface HopService {

    public Integer addHop(List<HopEntity> hopEntityList);

    public Integer updateHop(HopEntity hopEntity);

    public Integer deleteBatch(List<Integer> ids);

    public List<HopEntity> queryByFlowId(Integer flowId);

}
