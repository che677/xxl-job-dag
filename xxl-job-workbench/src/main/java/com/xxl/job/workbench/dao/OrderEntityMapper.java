package com.xxl.job.workbench.dao;

import com.xxl.job.core.biz.model.OrderEntity;
import java.util.List;

public interface OrderEntityMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(OrderEntity record);

    OrderEntity selectByPrimaryKey(Integer id);

    List<OrderEntity> selectAll();

    int updateByPrimaryKey(OrderEntity record);
}