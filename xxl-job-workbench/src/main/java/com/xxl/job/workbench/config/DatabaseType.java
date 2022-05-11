package com.xxl.job.workbench.config;

public enum DatabaseType {

    master("master", "master"),
    slave1("slave1", "slave1"),
    slave2("slave2","slave2");

    private String name;
    private String value;

    DatabaseType(String name, String value){
        this.name = name;
        this.value = value;
    }

    public String getName(){
        return name;
    }

    public String getValue(){
        return value;
    }

}
