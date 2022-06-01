package com.xxl.job.core.thread;

import lombok.Data;

import java.sql.Connection;
import java.sql.PreparedStatement;

@Data
public class OldDataTransThread extends Thread {

    PreparedStatement preparedStatement;
    Connection tConn;

    public OldDataTransThread(PreparedStatement preparedStatement, Connection tConn){
        this.preparedStatement = preparedStatement;
        this.tConn = tConn;
    }

    @Override
    public void run() {
        try{
            preparedStatement.executeBatch();
            preparedStatement.clearBatch();
            tConn.commit();
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("=================================================="+System.currentTimeMillis());
    }
}
