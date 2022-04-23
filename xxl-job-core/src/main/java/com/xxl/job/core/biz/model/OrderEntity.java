package com.xxl.job.core.biz.model;

import lombok.Data;

import java.util.Date;

@Data
public class OrderEntity {

    Integer id;
    String orderNo;
    Date createTime;

}
