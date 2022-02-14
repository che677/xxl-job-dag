package com.xxl.job.admin.core.dag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 连线信息
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hop {

    Long id;

    Long flowId;

    // 起始节点
    Long sourceId;
    // 终止节点
    Long targetId;

}
