package com.xxl.job.admin.dao;

import com.xxl.job.core.biz.model.HopEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HopMapper {
    int deleteByPrimaryKey(Integer id);
    int deleteBatch(List<Integer> ids);

    int insert(HopEntity record);
    int insertBatch(List<HopEntity> hopEntityList);

    List<HopEntity> queryByFlowId(Integer flowId);

    HopEntity selectByPrimaryKey(Integer id);

    List<HopEntity> selectAll();

    int updateByPrimaryKey(HopEntity record);
}