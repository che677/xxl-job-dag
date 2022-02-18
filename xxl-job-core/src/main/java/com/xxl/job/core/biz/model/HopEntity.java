package com.xxl.job.core.biz.model;

import lombok.Data;

@Data
public class HopEntity {
    private Integer id;

    private Integer flowId;

    private Integer sourceId;

    private Integer targetId;

}