package com.xxl.job.executor.service.jobhandler;

import com.cloudera.sqoop.SqoopOptions;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;

import org.apache.sqoop.tool.ImportTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * XxlJob开发示例（Bean模式）
 *
 * 开发步骤：
 *      1、任务开发：在Spring Bean实例中，开发Job方法；
 *      2、注解配置：为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 *      3、执行日志：需要通过 "XxlJobHelper.log" 打印执行日志；
 *      4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "XxlJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 *
 * @author xuxueli 2019-12-11 21:52:51
 */
@Slf4j
@Component
public class SampleXxlJob {

    private static Logger logger = LoggerFactory.getLogger(SampleXxlJob.class);


    /**
     * 1、简单任务示例（Bean模式）
     */
    @XxlJob("demoJobHandler")
    public void demoJobHandler() throws Exception {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        String startTime = df.format(new Date());
        String command = XxlJobHelper.getJobParam();
        log.error("XXL-JOB, Hello World."+"\n"+command);
        for (int i = 0; i < 5; i++) {
            log.error("beat at:" + i);
            TimeUnit.SECONDS.sleep(2);
        }
        String endTime = df.format(new Date());
        XxlJobHelper.handleSuccess(startTime+"       "+endTime);
        // default success
    }

    // 事件触发器
    @XxlJob("eventHandler")
    public void eventHandler() throws Exception {
        log.info("开始执行流程编排");

    }

    /**
     * 2、分片广播任务
     */
    @XxlJob("shardingJobHandler")
    public void shardingJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

        // 业务逻辑
        for (int i = 0; i < shardTotal; i++) {
            if (i == shardIndex) {
                XxlJobHelper.log("第 {} 片, 命中分片开始处理", i);
            } else {
                XxlJobHelper.log("第 {} 片, 忽略", i);
            }
        }

    }


    /**
     * 3、命令行任务
     */
    @XxlJob("commandJobHandler")
    public void commandJobHandler() throws Exception {
        String command = XxlJobHelper.getJobParam();
        int exitValue = -1;

        BufferedReader bufferedReader = null;
        try {
            // command process
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            //Process process = Runtime.getRuntime().exec(command);

            BufferedInputStream bufferedInputStream = new BufferedInputStream(process.getInputStream());
            bufferedReader = new BufferedReader(new InputStreamReader(bufferedInputStream));

            // command log
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                XxlJobHelper.log(line);
            }

            // command exit
            process.waitFor();
            exitValue = process.exitValue();
        } catch (Exception e) {
            XxlJobHelper.log(e);
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }

        if (exitValue == 0) {
            // default success
        } else {
            XxlJobHelper.handleFail("command exit value("+exitValue+") is failed");
        }

    }


    /**
     * 4、跨平台Http任务
     *  参数示例：
     *      "url: http://www.baidu.com\n" +
     *      "method: get\n" +
     *      "data: content\n";
     */
    @XxlJob("httpJobHandler")
    public void httpJobHandler() throws Exception {

        // param parse
        String param = XxlJobHelper.getJobParam();
        if (param==null || param.trim().length()==0) {
            XxlJobHelper.log("param["+ param +"] invalid.");

            XxlJobHelper.handleFail();
            return;
        }

        String[] httpParams = param.split("\n");
        String url = null;
        String method = null;
        String data = null;
        for (String httpParam: httpParams) {
            if (httpParam.startsWith("url:")) {
                url = httpParam.substring(httpParam.indexOf("url:") + 4).trim();
            }
            if (httpParam.startsWith("method:")) {
                method = httpParam.substring(httpParam.indexOf("method:") + 7).trim().toUpperCase();
            }
            if (httpParam.startsWith("data:")) {
                data = httpParam.substring(httpParam.indexOf("data:") + 5).trim();
            }
        }

        // param valid
        if (url==null || url.trim().length()==0) {
            XxlJobHelper.log("url["+ url +"] invalid.");

            XxlJobHelper.handleFail();
            return;
        }
        if (method==null || !Arrays.asList("GET", "POST").contains(method)) {
            XxlJobHelper.log("method["+ method +"] invalid.");

            XxlJobHelper.handleFail();
            return;
        }
        boolean isPostMethod = method.equals("POST");

        // request
        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;
        try {
            // connection
            URL realUrl = new URL(url);
            connection = (HttpURLConnection) realUrl.openConnection();

            // connection setting
            connection.setRequestMethod(method);
            connection.setDoOutput(isPostMethod);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(5 * 1000);
            connection.setConnectTimeout(3 * 1000);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");

            // do connection
            connection.connect();

            // data
            if (isPostMethod && data!=null && data.trim().length()>0) {
                DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
                dataOutputStream.write(data.getBytes("UTF-8"));
                dataOutputStream.flush();
                dataOutputStream.close();
            }

            // valid StatusCode
            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                throw new RuntimeException("Http Request StatusCode(" + statusCode + ") Invalid.");
            }

            // result
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            String responseMsg = result.toString();

            XxlJobHelper.log(responseMsg);

            return;
        } catch (Exception e) {
            XxlJobHelper.log(e);

            XxlJobHelper.handleFail();
            return;
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e2) {
                XxlJobHelper.log(e2);
            }
        }

    }

    /**
     * 5、生命周期任务示例：任务初始化与销毁时，支持自定义相关逻辑；
     */
    @XxlJob(value = "demoJobHandler2", init = "init", destroy = "destroy")
    public void demoJobHandler2() throws Exception {
        XxlJobHelper.log("XXL-JOB, Hello World.");
    }
    public void init(){
        logger.info("init");
    }
    public void destroy(){
        logger.info("destory");
    }

    /**
     * hive算子
     */
    @XxlJob(value="hive")
    public void hive(){
        String sql = XxlJobHelper.getJobParam();
        List<Integer> docIds = new ArrayList<Integer>();
        try {
            Class.forName("org.apache.hive.jdbc.HiveDriver");
            Connection conn = DriverManager.getConnection("jdbc:hive2://localhost:10000/default", "root", "123456");
            String[] sqlBatch = sql.split(";");
            Statement stmt = conn.createStatement();
            for(String s:sqlBatch){
                stmt.execute(s);
            }
            XxlJobHelper.handleSuccess("hive sql执行成功");
        }catch (Exception e){
            e.printStackTrace();
            XxlJobHelper.log(e);
            XxlJobHelper.handleFail(e.getMessage());
        }
    }

    /**
     * sqoop算子
     */
    @XxlJob(value="sqoop")
    public void sqoop() throws Exception {
        String JOB_NAME = "Sqoop Hive Job";
        String MAPREDUCE_JOB = "Hive Map Reduce Job";
        String DBURL ="jdbc:mysql://localhost:3306/sqoop";
        String DRIVER = "com.mysql.cj.jdbc.Driver";
        String USERNAME = "root";
        String PASSWORD = "123456";
        String HADOOP_HOME ="/opt/hadoop-2.9.2";
        String JAR_OUTPUT_DIR = "/data";
        String HIVE_HOME = "/opt/hive-2.7.3";
        String HIVE_DIR = "/user/hive/warehouse/";
        String WAREHOUSE_DIR = "hdfs://localhost:9000/user/hive/warehouse/mydb.db";
        String SUCCESS = "SUCCESS !!!";
        String FAIL = "FAIL !!!";
        String table = "goodtbl";

        SqoopOptions options = new SqoopOptions();
        options.setConnectString(DBURL);
        options.doOverwriteHiveTable();
        options.setTableName(table);
        options.setDriverClassName(DRIVER);
        options.setUsername(USERNAME);
        options.setPassword(PASSWORD);
        options.setHadoopMapRedHome(HADOOP_HOME);
        /* Hive connection parameters */
        options.setHiveHome(HIVE_HOME);
        options.setHiveImport(true);
        options.setHiveTableName("goodtbl");
        options.setOverwriteHiveTable(true);
        options.setFailIfHiveTableExists(false);
        //options.setFieldsTerminatedBy(',');
        options.setDirectMode(true);
        options.setNumMappers(1); // No. of Mappers to be launched for the job
        options.setWarehouseDir(WAREHOUSE_DIR);
        options.setJobName(JOB_NAME);
        options.setMapreduceJobName(MAPREDUCE_JOB);
        options.setTableName(table);
        options.setJarOutputDir(JAR_OUTPUT_DIR);
        System.out.println("Import Tool running ....");
        ImportTool it = new ImportTool();
        try{
            int retVal = it.run(options);
            if(retVal == 1){
                XxlJobHelper.handleSuccess("执行成功");
            }else{
                XxlJobHelper.handleFail("执行失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            XxlJobHelper.handleFail("执行失败:  "+e);
        }


    }

}
