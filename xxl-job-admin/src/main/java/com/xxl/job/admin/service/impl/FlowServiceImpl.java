package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.core.dag.Flow;
import com.xxl.job.admin.core.dag.Hop;
import com.xxl.job.admin.core.dag.TaskSet;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.dao.FlowMapper;
import com.xxl.job.admin.dao.HopMapper;
import com.xxl.job.admin.dao.TaskSetMapper;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.service.FlowService;
import com.xxl.job.core.biz.model.FlowEntity;
import com.xxl.job.core.biz.model.HopEntity;
import com.xxl.job.core.util.beanutil.ListBeanUtils;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FlowServiceImpl implements FlowService {

    @Resource
    private XxlJobInfoDao xxlJobInfoDao;
    @Resource
    private FlowMapper flowMapper;
    @Resource
    private HopMapper hopMapper;
    @Resource
    private TaskSetMapper taskSetMapper;

    public Integer addFlow(FlowEntity flowEntity){
            return flowMapper.insert(flowEntity);
    }

    public Integer deleteFlow(Integer id){
        return flowMapper.deleteByPrimaryKey(id);
    }

    public Integer updateFlow(FlowEntity flow){
        return flowMapper.updateByPrimaryKey(flow);
    }

    public List<FlowEntity> queryAllFlow(){
        return flowMapper.selectAll();
    }

    /**
     * 按照工作流来执行flow
     * @param flowId
     * @return
     */
    @Override
    public List<TaskSet> executeFlow(int flowId){
        FlowEntity flowEntity = flowMapper.selectByPrimaryKey(flowId);
        Flow flow = new Flow();
        String nodeList = flowEntity.getNodeList();
        if(StringUtils.isEmpty(nodeList)){
            return new ArrayList<>();
        }
        String[] split = nodeList.split(",");
        List<XxlJobInfo> nodeEntities = new ArrayList<>();
        for(String s:split){
            nodeEntities.add(xxlJobInfoDao.loadById(Integer.valueOf(s)));
        }
        List<HopEntity> hopEntities = hopMapper.queryByFlowId(flowId);
        List<Hop> hopList = ListBeanUtils.copyList(hopEntities, Hop::new);
        flow.setNodeList(nodeEntities);
        flow.setHopList(hopList);
        flow.setId(flowEntity.getId());
        flow.setJobId(flowEntity.getJobId());
        return getTaskSet(flow);
    }

    /**
     * 将任务编排后落库
     * @param flow
     * @return
     * @throws
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TaskSet> getTaskSet(Flow flow){
        // 将结果整合为一个完整的taskSet，并落入数据库
        List<TaskSet> res = new ArrayList<>();
        List<XxlJobInfo> updates = new ArrayList<>();
        // Hop表本质上是一个DAG，可以通过拓扑排序得到层级关系分明的执行顺序，然后进行计算即可
        // 将节点放入map，方便根据id进行检索
        Map<Integer, XxlJobInfo> nodeMap = new HashMap();
        flow.getNodeList().stream().forEach(r ->{
            nodeMap.put(r.getId(), r);
        });
        // 然后依据拓扑排序算法，生成执行顺序
        // 1、创建图算法的邻接表
        Map<Integer, List<Integer>> paths = new HashMap<>();
        flow.getHopList().stream().forEach(r -> {
            // 统计每个节点的入度,如果在target侧出现就加1
            nodeMap.get(r.getTargetId()).addInDegree(1);
            // 生成邻接表，如果不存在就新增，如果存在就加入新节点
            if(!paths.containsKey(r.getSourceId())){
                paths.put(r.getSourceId(),new ArrayList<>());
            }
            paths.get(r.getSourceId()).add(r.getTargetId());
        });
        // 2、检查哪些节点的入度为0，将其加入阻塞队列，后面可以分stage排队阻塞执行，直到返回成功消息
        // 如果nodeMap依然存在节点，就无限循环来删除节点
        int oldNum = nodeMap.size();
        int num = oldNum;
        while(num > 0){
            // 分不同的taskset，插入不同入度的节点；相同taskset的节点可以并发执行
            TaskSet taskSet = new TaskSet();
            taskSet.setId(UUID.randomUUID().toString());
            // for循环中注意，不允许直接改变list，如果要改，需要用迭代器操作
            // 将入度为0的节点单独存起来，对它们进行一轮处理
            List<Integer> zeroNodes = new ArrayList<>();
            for(Integer nodeId:nodeMap.keySet()) {
                XxlJobInfo node = nodeMap.get(nodeId);
                if(0 == node.getInDegree()) {
                    zeroNodes.add(nodeId);
                }
            }
            // 如果节点已经被插入了执行队列，而且入度为 0
            // (1)标记已处理的node总数num减1
            // (2)在hop中去除target方向的入度
            // (3)在执行顺序中增加node
            for(Integer nodeId:zeroNodes){
                XxlJobInfo node = nodeMap.get(nodeId);
                num -= 1;
                // 该节点指向的下一级节点的集合，每一个节点的入度都要减1;如果是最后一个节点，就不需要减1
                List<Integer> targets = paths.get(nodeId);
                if(!CollectionUtils.isEmpty(targets)){
                    targets.stream().forEach(r -> {nodeMap.get(r).addInDegree(-1);});
                }
                taskSet.addNode(node);
                node.setTaskSetId(taskSet.getId());
                updates.add(node);
                // nodeMap集合删除该节点，因为他指向的节点已经被放入
                nodeMap.remove(nodeId);
            }
            // 如果本轮循环，没有出现入度为0的节点，则必定存在环，需要直接返回失败
            if(num == oldNum){
                log.error("出现环");
                throw new RuntimeException();
            }
            oldNum = num;
            // 已经得到了所有的节点，而且不存在环，可以将任务集加入缓存队列
            // taskScheduler已经初始化了executor，一直在进行任务的不断调用
            taskSet.setJobId(taskSet.getNodeList().stream().map(r -> String.valueOf(r.getId()))
                    .collect(Collectors.joining(",")));
            // 为上一级任务集添加连接到本任务集
            if(res.size()>0){
                res.get(res.size()-1).setNextId(taskSet.getId());
            }else if(res.size() == 0){
                taskSet.setIsFirst(1);
            }
            taskSet.setTriggerTime(new Date()); // 获取当前时间，便于后续筛选日志信息
            taskSet.setFlowId(flow.getId());
            res.add(taskSet);
        }
        // 任务执行完毕，将taskset集合落库
        taskSetMapper.deleteByFlowId(flow.getId());
        int i = taskSetMapper.batchInsert(res);
        updates.stream().forEach(r->xxlJobInfoDao.updateTask(r));
//        int j = xxlJobInfoDao.updateTask(updates);
        return res;
    }
}
