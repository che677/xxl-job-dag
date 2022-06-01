package com.xxl.job.core.biz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MqEntity {

    String messageId;
    String content;
    String toExchange;
    String classType;
    Integer messageStatus;
    Date createTime;
    Date updateTime;


}
