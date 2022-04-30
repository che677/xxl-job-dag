package com.xxl.job.workbench.service.impl;

import com.xxl.job.core.biz.model.OrderEntity;
import com.xxl.job.core.dubbo.OrderService;
import com.xxl.job.workbench.dao.OrderEntityMapper;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;

@Service
@org.springframework.stereotype.Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderEntityMapper orderEntityMapper;

    @Override
    public Boolean saveOrder() {
        OrderEntity entity = new OrderEntity();
        entity.setOrderNo(UUID.randomUUID().toString());
        entity.setCreateTime(new Date());
        return orderEntityMapper.insert(entity) == 1?true:false;
    }
}
