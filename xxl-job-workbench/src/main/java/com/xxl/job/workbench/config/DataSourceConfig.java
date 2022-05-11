package com.xxl.job.workbench.config;

import com.zaxxer.hikari.HikariDataSource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@MapperScan(basePackages = "com.xxl.job.workbench.dao")
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
//@PropertySource("classpath:")
public class DataSourceConfig {
    @Value("${spring.datasource.master.driver-class-name}")
    private String masterDriver;
    @Value("${spring.datasource.master.url}")
    private String masterUrl;
    @Value("${spring.datasource.master.username}")
    private String masterUsername;
    @Value("${spring.datasource.master.password}")
    private String masterPassword;

    @Value("${spring.datasource.slave1.driver-class-name}")
    private String slave1Driver;
    @Value("${spring.datasource.slave1.url}")
    private String slave1Url;
    @Value("${spring.datasource.slave1.username}")
    private String slave1Username;
    @Value("${spring.datasource.slave1.password}")
    private String slave1Password;

    @Value("${spring.datasource.slave2.driver-class-name}")
    private String slave2Driver;
    @Value("${spring.datasource.slave2.url}")
    private String slave2Url;
    @Value("${spring.datasource.slave2.username}")
    private String slave2Username;
    @Value("${spring.datasource.slave2.password}")
    private String slave2Password;

    @Bean("masterDataSource")
    public DataSource masterDataSource(){
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(masterDriver);
        dataSource.setJdbcUrl(masterUrl);
        dataSource.setUsername(masterUsername);
        dataSource.setPassword(masterPassword);
        return dataSource;
    }

    @Bean("slave1DataSource")
    public DataSource slave1DataSource(){
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(slave1Driver);
        dataSource.setJdbcUrl(slave1Url);
        dataSource.setUsername(slave1Username);
        dataSource.setPassword(slave1Password);
        return dataSource;
    }

    @Bean("slave2DataSource")
    public DataSource slave2DataSource(){
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(slave2Driver);
        dataSource.setJdbcUrl(slave2Url);
        dataSource.setUsername(slave2Username);
        dataSource.setPassword(slave2Password);
        return dataSource;
    }

    @Bean
    @Primary
    public DataSource dynamicDataSource() {
        Map<Object, Object> dataSourceMap = new HashMap<>(2);
        dataSourceMap.put(DatabaseType.master, masterDataSource());
        dataSourceMap.put(DatabaseType.slave1, slave1DataSource());
        dataSourceMap.put(DatabaseType.slave2, slave2DataSource());
        //设置动态数据源
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setTargetDataSources(dataSourceMap);
        dynamicDataSource.setDefaultTargetDataSource(masterDataSource());

        return dynamicDataSource;
    }

}
