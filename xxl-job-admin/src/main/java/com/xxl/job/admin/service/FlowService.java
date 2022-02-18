package com.xxl.job.admin.service;

import com.xxl.job.admin.core.dag.Flow;
import com.xxl.job.admin.core.dag.TaskSet;

import java.util.List;

public interface FlowService {

    List<TaskSet> executeFlow(int flowId);

    List<TaskSet> getTaskSet(Flow flow);

}
