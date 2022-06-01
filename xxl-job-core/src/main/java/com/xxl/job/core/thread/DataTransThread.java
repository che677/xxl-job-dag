package com.xxl.job.core.thread;

import com.xxl.job.core.util.JdbcUtil;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;

@Data
public class DataTransThread extends Thread {

    PreparedStatement preparedStatement;
    Connection tConn;

    public DataTransThread(PreparedStatement preparedStatement, Connection tConn){
        this.preparedStatement = preparedStatement;
        this.tConn = tConn;
    }

    @Override
    public void run() {
        try{
            tConn.setAutoCommit(false);
            preparedStatement.executeBatch();
            preparedStatement.clearBatch();
            tConn.commit();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try{
                if(null != tConn && null != preparedStatement){
                    tConn.close();
                    preparedStatement.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            System.out.println("=================================================="+System.currentTimeMillis());
        }
    }
}
