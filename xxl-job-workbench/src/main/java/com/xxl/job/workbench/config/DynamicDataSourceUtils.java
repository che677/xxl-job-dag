package com.xxl.job.workbench.config;

import java.util.Random;

public class DynamicDataSourceUtils {

    public static void chooseBasicDataSource(){
        DatabaseContextHolder.setDatabaseType(DatabaseType.master);
    }

    public static void chooseBranchDataSource(){
        Random Ran = new Random();//生成Random对象
        int i = Ran.nextInt(100);
        if(i%2 == 0){
            DatabaseContextHolder.setDatabaseType(DatabaseType.slave1);
        }else{
            DatabaseContextHolder.setDatabaseType(DatabaseType.slave2);
        }

    }

}
