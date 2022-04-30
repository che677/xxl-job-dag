package com.xxl.job.core.biz.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CategoryEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 分类id
     */
    private Long catId;
    /**
     * 分类名称
     */
    private String name;
    /**
     * 父分类id
     */
    private Long parentCid;
    /**
     * 层级
     */
    private Integer catLevel;
    /**
     * 是否显示[0-不显示，1显示]
     */
    private Integer showStatus;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 图标地址
     */
    private String icon;
    /**
     * 计量单位
     */
    private String productUnit;
    /**
     * 商品数量
     */
    private Integer productCount;

    /**
     * 所有子分类
     */
    private List<CategoryEntity> children;

}
