package com.xxl.job.admin.core.dag;

import com.alice.workbench.model.strategy.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模板信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Flow {

    Long id;
    // 节点信息
    List<Node> nodeList;
    // 连线信息
    List<Hop> hopList;
    // 定时调度任务ID
    Long jobId;

}
