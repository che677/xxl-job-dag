package com.xxl.job.admin.dao;

import com.xxl.job.core.biz.model.AccountEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
@Mapper
public interface AccountEntityMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(AccountEntity record);

    AccountEntity selectByPrimaryKey(Integer id);

    List<AccountEntity> selectAll();

    int updateByPrimaryKey(AccountEntity record);
}