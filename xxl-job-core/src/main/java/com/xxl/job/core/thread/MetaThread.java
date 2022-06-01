package com.xxl.job.core.thread;

import lombok.Data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class MetaThread extends Thread {

    Connection tConn;

    String sql;

    public MetaThread(Connection tConn, String sql){
        this.tConn = tConn;
        this.sql = sql;
    }

    @Override
    public void run() {
        PreparedStatement pStmt = null;    //定义盛装SQL语句的载体pStmt
        ResultSet rs = null;    //定义查询结果集rs
        List<Map<String, Object>> list = new ArrayList<>();
        try{
            pStmt = tConn.prepareStatement(sql);    //<第4步>获取盛装SQL语句的载体pStmt
            rs = pStmt.executeQuery();    //<第5步>获取查询结果集rs
            if(rs != null){
                try {
                    //数据库列名
                    ResultSetMetaData data= rs.getMetaData();
                    //遍历结果   getColumnCount 获取表列个数
                    while (rs.next()) {
                        Map<String, Object> map = new HashMap<>();
                        for(int i=1;i<=data.getColumnCount();i++){
                            // typeName 字段名 type 字段类型
                            map.put(data.getColumnTypeName(i), data.getColumnType(i));
                            list.add(map);
                        }
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    rs.close();    //<第6步>关闭结果集
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println(list);
        System.out.println("=================================================="+System.currentTimeMillis());
    }
}
