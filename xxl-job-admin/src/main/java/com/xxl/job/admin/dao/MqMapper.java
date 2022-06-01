package com.xxl.job.admin.dao;

import com.xxl.job.core.biz.model.MqEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MqMapper {
    int deleteByPrimaryKey(String messageId);

    int insert(MqEntity record);

    MqEntity selectByPrimaryKey(String messageId);

    List<MqEntity> selectAll();

    int updateByPrimaryKey(MqEntity record);
}