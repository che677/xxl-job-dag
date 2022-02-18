package com.xxl.job.admin.core.dag;

import com.xxl.job.admin.core.model.XxlJobInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskSet{
    Integer flowId;
    int isFirst = 0;
    String id;
    // 下一个数据集的id
    String nextId;

    // 任务，一个是字符串与数据库交互，一个是变量
    String jobId;
    List<XxlJobInfo> nodeList = new ArrayList<>();

    Date triggerTime;

    public void addNode(XxlJobInfo node){
        nodeList.add(node);
    }
}
