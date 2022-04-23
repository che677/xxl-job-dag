package com.xxl.job.workbench.service;

import com.xxl.job.core.biz.model.FlowEntity;

import java.util.List;

public interface FlowService {

    FlowEntity executeFlow(int flowId);

}
