package com.xxl.job.workbench.dao;

import com.xxl.job.core.biz.model.OrderEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderEntityMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(OrderEntity record);

    OrderEntity selectByPrimaryKey(Integer id);

    List<OrderEntity> selectAll();

    int updateByPrimaryKey(OrderEntity record);
}