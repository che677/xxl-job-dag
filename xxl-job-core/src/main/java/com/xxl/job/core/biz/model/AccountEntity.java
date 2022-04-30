package com.xxl.job.core.biz.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class AccountEntity implements Serializable {
    private static final long serialVersionUID = 128394182938992384L;
    Integer id;
    String name;
    Integer balance;
}
