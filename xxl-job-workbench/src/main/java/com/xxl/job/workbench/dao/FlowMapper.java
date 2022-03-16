package com.xxl.job.workbench.dao;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FlowMapper {

    int cas(int id);

}
