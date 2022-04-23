package com.xxl.job.admin.dao;

import com.xxl.job.core.biz.model.AccountEntity;
import java.util.List;

public interface AccountEntityMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(AccountEntity record);

    AccountEntity selectByPrimaryKey(Integer id);

    List<AccountEntity> selectAll();

    int updateByPrimaryKey(AccountEntity record);
}