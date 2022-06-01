package com.xxl.job.workbench.service.impl;

import com.xxl.job.core.thread.DataTransThread;
import com.xxl.job.core.thread.MetaThread;
import com.xxl.job.core.thread.OldDataTransThread;
import com.xxl.job.workbench.config.DataSourceConfig;
import com.xxl.job.workbench.config.MyThreadConfig;
import com.xxl.job.workbench.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class DataServiceImpl implements DataService {

    @Autowired
    DataSourceConfig dataSourceConfig;
    @Autowired
    private MyThreadConfig myThreadConfig;

    @Override
    public void transData(int type) {
        DataSource source = dataSourceConfig.slave1DataSource();
        DataSource target = dataSourceConfig.slave2DataSource();
        Connection sConn = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Connection tConn = null;
        PreparedStatement preparedStatement = null;
        String querySql = "select * from goodtbl";
        String insertSql = "insert into goodtbl (id, gname, price,stock_number,create_time) values (?,?,?,?,?)";
        ThreadPoolExecutor threadPoolExecutor = myThreadConfig.threadPoolExecutor();
        try{
            System.out.println("=================================================="+System.currentTimeMillis());
            // 获取源数据
            sConn = source.getConnection();
            statement = sConn.prepareStatement(querySql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            // 批量读取，一次5000条
            statement.setFetchSize(5000);
            resultSet = statement.executeQuery();
            // 获取目标数据
            tConn = target.getConnection();
            preparedStatement = tConn.prepareStatement(
                    insertSql,
                    ResultSet.TYPE_FORWARD_ONLY,  // 只能向前读取
                    ResultSet.CONCUR_READ_ONLY);    // 设为只读类型的
            // 关闭目标数据库连接的自动提交
            tConn.setAutoCommit(false);
            int count = 0;
            while(resultSet.next()){
                Connection newConn = null;
                PreparedStatement newPrep = null;
                newConn = target.getConnection();
                newPrep = tConn.prepareStatement(
                            insertSql,
                            ResultSet.TYPE_FORWARD_ONLY,  // 只能向前读取
                            ResultSet.CONCUR_READ_ONLY);
                int columnCount = resultSet.getMetaData().getColumnCount();
                for (int i = 1; i <= columnCount; ++i) {
                    if(type == 1){
                        try{
                            newPrep.setObject(i, resultSet.getObject(i));
                        }catch (Exception e){
                            System.out.println(count);
                        }

                    }else{
                        preparedStatement.setObject(i, resultSet.getObject(i));
                    }
                }
                /* 添加批次 */
                if(type == 1){
                    newPrep.addBatch();
                }else{
                    preparedStatement.addBatch();
                }
                if (count % 5000 == 0 && count!=0) {
                    if(type == 1){
                        threadPoolExecutor.execute(new DataTransThread(newPrep,
                                newConn));
                        System.out.println("new Thread");
                    }else if(type == 2){
                        // 创建新线程，不创建新链接
                        threadPoolExecutor.execute(new OldDataTransThread(preparedStatement, tConn));
                        System.out.println("new Thread");
                    }else{
                        // 单线程处理
                        preparedStatement.executeBatch();
                        preparedStatement.clearBatch();
                        tConn.commit();
                    }
                }
                ++count;
            }
            // 一个批次没有提交完成的，继续提交数据
            if (count % 5000 != 0 && count != 1) {
                preparedStatement.executeBatch();
                preparedStatement.clearBatch();
                tConn.commit();
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            JdbcUtils.closeConnection(sConn);
            JdbcUtils.closeConnection(tConn);
            JdbcUtils.closeResultSet(resultSet);
            JdbcUtils.closeStatement(statement);
            JdbcUtils.closeStatement(preparedStatement);
            System.out.println("=================================================="+System.currentTimeMillis());
        }
    }

    @Override
    public void transMeta() throws SQLException {
        String sql = "select * from user limit 1";    //定义查询的SQL语句
        String sql2 = "select * from goodtbl limit 1";
        Connection conn = null;
        try{
            DataSource source = dataSourceConfig.slave1DataSource();
            conn = source.getConnection();
            new MetaThread(conn, sql).start();
            new MetaThread(conn, sql2).start();
        }catch (Exception e){

        }
    }
}
