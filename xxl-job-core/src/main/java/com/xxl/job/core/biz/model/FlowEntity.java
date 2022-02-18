package com.xxl.job.core.biz.model;

import lombok.Data;

@Data
public class FlowEntity {
    private Integer id;

    private Integer jobId;

    private String nodeList;

    private String hopList;

}