package com.xxl.job.workbench.service;

import com.xxl.job.core.biz.model.CategoryEntity;
import com.xxl.job.core.biz.model.Catelog2Vo;
import com.xxl.job.core.biz.model.ReturnT;

import java.util.List;
import java.util.Map;

public interface CategoryService {

    List<CategoryEntity> listWithTree();

    String testRedis();

    Map<String, List<Catelog2Vo>> getCatalogJson();

    void updateCatalog(Long catId) throws InterruptedException;

    List<CategoryEntity> getLevel1();

    void readwrite();

    void collectData();
}
