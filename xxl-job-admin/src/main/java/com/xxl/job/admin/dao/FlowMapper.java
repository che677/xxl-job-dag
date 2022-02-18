package com.xxl.job.admin.dao;

import com.xxl.job.core.biz.model.FlowEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FlowMapper {
    int deleteByPrimaryKey(int id);

    int insert(FlowEntity record);

    FlowEntity selectByPrimaryKey(int id);

    List<FlowEntity> selectAll();

    int updateByPrimaryKey(FlowEntity record);
}