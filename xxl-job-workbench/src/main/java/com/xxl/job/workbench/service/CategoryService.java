package com.xxl.job.workbench.service;

import com.xxl.job.core.biz.model.CategoryEntity;
import com.xxl.job.core.biz.model.Catelog2Vo;

import java.util.List;
import java.util.Map;

public interface CategoryService {

    List<CategoryEntity> listWithTree();

    String testRedis();

    Map<String, List<Catelog2Vo>> getCatalogJson();
}
