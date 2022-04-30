package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.dao.AccountEntityMapper;
import com.xxl.job.core.biz.model.AccountEntity;
import com.xxl.job.core.dubbo.AccountService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@org.apache.dubbo.config.annotation.Service
@Service
public class AccountServiceImpl implements AccountService {

    @Resource
    private AccountEntityMapper accountMapper;

    @Override
    public Boolean saveOrUpdateAccount() {
        AccountEntity accountEntity = accountMapper.selectByPrimaryKey(1);
        accountEntity.setBalance(accountEntity.getBalance()-100);
        return accountMapper.updateByPrimaryKey(accountEntity) == 1?true:false;
    }

}
