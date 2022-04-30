package com.xxl.job.core.biz.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class OrderEntity implements Serializable {

    private static final long serialVersionUID = 4728347823842L;

    Integer id;
    String orderNo;
    Date createTime;

}
