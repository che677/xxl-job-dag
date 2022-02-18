package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.dag.TaskSet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskSetMapper {

    int deleteByPrimaryKey(String id);
    int deleteByFlowId(Integer id);

    int insert(TaskSet record);
    int batchInsert(@Param("record") List<TaskSet> record);

    TaskSet selectByPrimaryKey(String id);

    List<TaskSet> selectAll();

    int updateByPrimaryKey(TaskSet record);

}
