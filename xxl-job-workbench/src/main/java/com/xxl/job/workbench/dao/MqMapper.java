package com.xxl.job.workbench.dao;

import com.xxl.job.core.biz.model.MqEntity;
import java.util.List;

public interface MqMapper {
    int deleteByPrimaryKey(String messageId);

    int insert(MqEntity record);

    MqEntity selectByPrimaryKey(String messageId);

    List<MqEntity> selectAll();

    int updateByPrimaryKey(MqEntity record);
}