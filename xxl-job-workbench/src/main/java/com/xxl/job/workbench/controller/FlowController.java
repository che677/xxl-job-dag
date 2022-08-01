package com.xxl.job.workbench.controller;

import com.alibaba.fastjson.JSON;
import com.xxl.job.core.biz.model.CategoryEntity;
import com.xxl.job.core.biz.model.Catelog2Vo;
import com.xxl.job.core.biz.model.FlowEntity;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.dubbo.AccountService;
import com.xxl.job.core.dubbo.OrderService;
import com.xxl.job.workbench.service.CategoryService;
import com.xxl.job.workbench.service.DataService;
import com.xxl.job.workbench.service.FlowService;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@RestController
public class FlowController {

    @Autowired
    private FlowService flowService;

    @Reference(check = false, retries = 0)
    private AccountService accountService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DataService dataService;

    @GetMapping("/getflow")
    public ReturnT<String> chartInfo(Integer flowId) {
        FlowEntity flowEntity = flowService.executeFlow(flowId);
        ReturnT<String> success = ReturnT.SUCCESS;
        success.setContent(JSON.toJSONString(flowEntity));
        return success;
    }

    @GetMapping("/getRedis")
    public String testRedis(){
        return categoryService.testRedis();
    }

    @GetMapping("/catalog")
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        Map<String, List<Catelog2Vo>> catalogJson = categoryService.getCatalogJson();
        return catalogJson;
    }

    @PostMapping("/updateOrder")
    public ReturnT<String> updateOrder(){
//        accountService.saveOrUpdateAccount();
        orderService.saveOrder();
        return ReturnT.SUCCESS;
    }

    @PostMapping("/listLevel1")
    public ReturnT<CategoryEntity> listLevel1(){
        List<CategoryEntity> level1 = categoryService.getLevel1();
        return new ReturnT(level1);
    }


    @PostMapping("/updateCatelog")
    public ReturnT<String> updateCatelog(Long catId) throws InterruptedException {
        categoryService.updateCatalog(catId);
        return ReturnT.SUCCESS;
    }

    @PostMapping("/transData")
    public ReturnT<String> transData(int type) throws InterruptedException {
        dataService.transData(type);
        return ReturnT.SUCCESS;
    }

    @PostMapping("/transMeta")
    public ReturnT<String> transData() throws InterruptedException, SQLException {
        dataService.transMeta();
        return ReturnT.SUCCESS;
    }

    @PostMapping("/readwrite")
    public ReturnT<String> readwrite() throws InterruptedException, SQLException {
        categoryService.readwrite();
        return ReturnT.SUCCESS;
    }

    @PostMapping("/collectData")
    public ReturnT<String> collectData(){
        categoryService.collectData();
        return ReturnT.SUCCESS;
    }

    @PostMapping("/delayQueue")
    public void delayQueue(){
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);
        scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("111");
            }
        }, 10, TimeUnit.SECONDS);
    }

    @PostMapping("/delayTest")
    public void delayTest(){
        Thread thread = null;
        try{
            FutureTask<Boolean> futureTask = new FutureTask<Boolean>(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    System.out.println("===");
                    Thread.sleep(15000);
                    return true;
                }
            });
            thread = new Thread(futureTask);
            thread.start();
            Boolean result = futureTask.get(10, TimeUnit.SECONDS);
            System.out.println(result);
        }catch (Exception e){
            System.out.println("报错");
        }finally {
            thread.interrupt();
        }
    }

}
