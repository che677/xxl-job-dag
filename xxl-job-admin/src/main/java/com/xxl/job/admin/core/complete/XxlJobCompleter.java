package com.xxl.job.admin.core.complete;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.dag.TaskSet;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.thread.JobCompleteHelper;
import com.xxl.job.admin.core.thread.JobTriggerPoolHelper;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.service.FlowService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.enums.SampleEnum;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author xuxueli 2020-10-30 20:43:10
 */
public class XxlJobCompleter {
    private static Logger logger = LoggerFactory.getLogger(XxlJobCompleter.class);

    /**
     * common fresh handle entrance (limit only once)
     *
     * @param xxlJobLog
     * @return
     */
    public static void updateHandleInfoAndFinish(XxlJobLog xxlJobLog) {

        // finish
        finishJob(xxlJobLog);

    }

    /**
     * do somethind to finish job
     */
    private static void finishJob(XxlJobLog xxlJobLog){

        // 1、handle success, to trigger child job
        String triggerChildMsg = null;
        String jobIds = null;
//        RAtomicLong atomicLong = null;
        if (XxlJobContext.HANDLE_COCE_SUCCESS == xxlJobLog.getHandleCode()) {
            XxlJobInfo xxlJobInfo = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(xxlJobLog.getJobId());
            // 链式依赖任务
            if (xxlJobInfo!=null && xxlJobInfo.getChildJobId()!=null && xxlJobInfo.getChildJobId().trim().length()>0) {
                triggerChildMsg = "<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>"+ I18nUtil.getString("jobconf_trigger_child_run") +"<<<<<<<<<<< </span><br>";

                String[] childJobIds = xxlJobInfo.getChildJobId().split(",");
                for (int i = 0; i < childJobIds.length; i++) {
                    int childJobId = (childJobIds[i]!=null && childJobIds[i].trim().length()>0 && isNumeric(childJobIds[i]))?Integer.valueOf(childJobIds[i]):-1;
                    if (childJobId > 0) {

                        JobTriggerPoolHelper.trigger(childJobId, TriggerTypeEnum.PARENT, -1, null, null, null);
                        ReturnT<String> triggerChildResult = ReturnT.SUCCESS;

                        // add msg
                        triggerChildMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg1"),
                                (i+1),
                                childJobIds.length,
                                childJobIds[i],
                                (triggerChildResult.getCode()==ReturnT.SUCCESS_CODE?I18nUtil.getString("system_success"):I18nUtil.getString("system_fail")),
                                triggerChildResult.getMsg());
                    } else {
                        triggerChildMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg2"),
                                (i+1),
                                childJobIds.length,
                                childJobIds[i]);
                    }
                }

            }

            // 这里是任务组的回调函数，执行DAG流程编排任务，并启动后续任务

            // 可以采用redis的原子类来实现，或者用分布式锁来实现
//            if(xxlJobInfo.getTaskSetId()!=null){
//                RedissonClient redisson = null;
//                try{
//                    redisson = XxlJobAdminConfig.getAdminConfig().getRedisson();
//                    atomicLong = redisson.getAtomicLong("taskset:" + xxlJobInfo.getFlowId());
//                }catch (Exception e){
//                    logger.error("无法添加计数器");
//                    return;
//                }
//                // 如果这里是起始触发节点，直接触发后续依赖任务即可
//                if(SampleEnum.EVENT_HANDLER.getTitle().equalsIgnoreCase(xxlJobInfo.getExecutorHandler())){
//                    // 针对任务组初始化节点执行流程编排；如果为了避免每次触发任务都重复生成taskset，也可以在保存flow工作流的时候执行本步骤
//                    List<TaskSet> taskSets = XxlJobAdminConfig.getAdminConfig().getFlowService().
//                            executeFlow(xxlJobInfo.getFlowId());
//                    // 获取第一个元素
//                    List<TaskSet> collect = taskSets.parallelStream().filter(r -> 1 == r.getIsFirst()).collect(Collectors.toList());
//                    if(CollectionUtils.isEmpty(collect)){
//                        // 没有第一个元素，有错误
//                        logger.error("找不到初始任务集");
//                        return;
//                    }
//                    String nextId = collect.get(0).getNextId();
//                    jobIds = XxlJobAdminConfig.getAdminConfig().getTaskSetMapper().selectByPrimaryKey(nextId).getJobId();
//                    String[] split = jobIds.split(",");
//                    atomicLong.set(split.length);
//                }else{
//                    // 如果这里是中间节点与末尾节点，先获取当前流程的原子类，看看任务状态是否是都已完成；
//                    long count = atomicLong.decrementAndGet();
//                    if(count == 0){
//                        // 如果是0的话，证明本轮任务完成，直接启动下一个taskset的程序，并添加下一轮的计数器
//                        TaskSet taskSet = XxlJobAdminConfig.getAdminConfig().getTaskSetMapper().selectByPrimaryKey(xxlJobInfo.getTaskSetId());
//                        TaskSet nextTaskSet = XxlJobAdminConfig.getAdminConfig().getTaskSetMapper().selectByPrimaryKey(taskSet.getNextId());
//                        jobIds = ObjectUtils.isEmpty(nextTaskSet)?null:nextTaskSet.getJobId();
//                        if(!StringUtils.isEmpty(jobIds)){
//                            String[] split = jobIds.split(",");
//                            atomicLong.set(split.length);
//                        }else{
//                            // 下游没有jobIds，证明任务组已经完成任务
//                            atomicLong.delete();
//                        }
//                    }else{
//                        //否则不予处理
//                        jobIds = null;
//                    }
//                }
//                // 再触发下游依赖任务
//                if(!StringUtils.isEmpty(jobIds)){
//                    for(String childJob:jobIds.split(",")){
//                        JobTriggerPoolHelper.trigger(Integer.valueOf(childJob), TriggerTypeEnum.PARENT,
//                                -1, null, null, null);
//                    }
//                }
//            }


            // 可以直接用MySQL做计数器，并发去update的时候由于存在写锁，不会产生一致性问题
            if(xxlJobInfo.getTaskSetId()!=null){
                FlowService flowService = null;
                try{
                    flowService = XxlJobAdminConfig.getAdminConfig().getFlowService();
                    flowService.updateCount(xxlJobInfo.getFlowId(), 0);
                }catch (Exception e){
                    logger.error("无法添加计数器",e);
                    return;
                }
                // 如果这里是起始触发节点，直接触发后续依赖任务即可
                if(SampleEnum.EVENT_HANDLER.getTitle().equalsIgnoreCase(xxlJobInfo.getExecutorHandler())){
                    // 针对任务组初始化节点执行流程编排；如果为了避免每次触发任务都重复生成taskset，也可以在保存flow工作流的时候执行本步骤
                    List<TaskSet> taskSets = XxlJobAdminConfig.getAdminConfig().getFlowService().
                            executeFlow(xxlJobInfo.getFlowId());
                    // 获取第一个元素
                    List<TaskSet> collect = taskSets.parallelStream().filter(r -> 1 == r.getIsFirst()).collect(Collectors.toList());
                    if(CollectionUtils.isEmpty(collect)){
                        // 没有第一个元素，有错误
                        logger.error("找不到初始任务集");
                        return;
                    }
                    String nextId = collect.get(0).getNextId();
                    jobIds = XxlJobAdminConfig.getAdminConfig().getTaskSetMapper().selectByPrimaryKey(nextId).getJobId();
                    String[] split = jobIds.split(",");
                    flowService.updateCount(xxlJobInfo.getFlowId(), split.length);
                }else{
                    // 如果这里是中间节点与末尾节点，先获取当前流程的计数，看看任务状态是否是都已完成；
                    long count = flowService.decrease(xxlJobInfo.getFlowId());
                    if(count == 0){
                        // 如果是0的话，证明countNum已经是0了，没有做修改，本轮任务完成，直接启动下一个taskset的程序，并添加下一轮的计数器
                        TaskSet taskSet = XxlJobAdminConfig.getAdminConfig().getTaskSetMapper().selectByPrimaryKey(xxlJobInfo.getTaskSetId());
                        TaskSet nextTaskSet = XxlJobAdminConfig.getAdminConfig().getTaskSetMapper().selectByPrimaryKey(taskSet.getNextId());
                        jobIds = ObjectUtils.isEmpty(nextTaskSet)?null:nextTaskSet.getJobId();
                        if(!StringUtils.isEmpty(jobIds)){
                            String[] split = jobIds.split(",");
                            flowService.updateCount(xxlJobInfo.getFlowId(),split.length);
                        }else{
                            // 下游没有jobIds，证明任务组已经完成任务
                            flowService.updateCount(xxlJobInfo.getFlowId(), 0);
                        }
                    }else{
                        //否则不予处理
                        jobIds = null;
                    }
                }
                // 再触发下游依赖任务
                if(!StringUtils.isEmpty(jobIds)){
                    for(String childJob:jobIds.split(",")){
                        JobTriggerPoolHelper.trigger(Integer.valueOf(childJob), TriggerTypeEnum.PARENT,
                                -1, null, null, null);
                    }
                }
            }
        }
        if (triggerChildMsg != null) {
            xxlJobLog.setHandleMsg( xxlJobLog.getHandleMsg() + triggerChildMsg );
        }
        // 2、fix_delay trigger next
        // on the way
        // text最大64kb 避免长度过长
        if (xxlJobLog.getHandleMsg().length() > 15000) {
            xxlJobLog.setHandleMsg( xxlJobLog.getHandleMsg().substring(0, 15000) );
        }

        // 如果都成功了，再写日志，然后再解锁
        XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateHandleInfo(xxlJobLog);
        // fresh handle
    }

    private static boolean isNumeric(String str){
        try {
            int result = Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
