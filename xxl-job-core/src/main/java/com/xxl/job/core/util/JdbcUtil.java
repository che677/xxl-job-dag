package com.xxl.job.core.util;

import java.sql.*;

public class JdbcUtil {

    // 注意：此处不能使用"org.apache.hive.jdbc.HiveDriver.class"，否则会报错。
    private static String driver = "org.apache.hive.jdbc.HiveDriver";

    // hive默认的端口是10000，default是要连接的hive的数据库的名称
    private static String url = "jdbc:hive2://localhost:10000/default";

    // 注册驱动
    static {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // 获取连接
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 释放资源
    public static void realese(Connection conn, Statement st, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                rs = null;    // 将某个对象设置为null，那么这个对象会迅速成为垃圾回收的对象
            }
        }

        if (st != null) {
            try {
                st.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                st = null;    // 将某个对象设置为null，那么这个对象会迅速成为垃圾回收的对象
            }
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                conn = null;    // 将某个对象设置为null，那么这个对象会迅速成为垃圾回收的对象
            }
        }
    }


}
