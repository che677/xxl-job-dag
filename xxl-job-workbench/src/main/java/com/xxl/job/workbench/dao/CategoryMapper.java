package com.xxl.job.workbench.dao;

import com.xxl.job.core.biz.model.CategoryEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper {

    List<CategoryEntity> selectList();

    Integer updateCount(Long catId);

}
