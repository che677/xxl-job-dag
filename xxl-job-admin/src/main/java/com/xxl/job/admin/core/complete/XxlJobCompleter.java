package com.xxl.job.admin.core.complete;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.dag.TaskSet;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.thread.JobCompleteHelper;
import com.xxl.job.admin.core.thread.JobTriggerPoolHelper;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobContext;
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
        RLock lock = null;
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
            if(xxlJobInfo.getFlowId()!=null){
                JobCompleteHelper helper = JobCompleteHelper.getInstance();
                // 如果这里是起始触发节点，直接触发后续依赖任务即可
                if("eventHandler".equalsIgnoreCase(xxlJobInfo.getExecutorHandler())){
                    // 针对任务组初始化节点执行流程编排
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
                }else{
                    // 此处先查询状态再更新，需要添加锁来保证原子性，锁的名称为 taskset
                    String taskSetId = xxlJobInfo.getTaskSetId();
                    try{
                        RedissonClient redisson = XxlJobAdminConfig.getAdminConfig().getRedisson();
                        lock = redisson.getLock("dag:"+taskSetId);
                        lock.lock(1000, TimeUnit.SECONDS);
                    }catch (Exception e){
                        logger.error("加锁失败");
                    }
                    // 如果这里是中间节点与末尾节点，先获取任务所在的taskset，看看任务状态是否是都已完成；
                    // 如果是的话，直接启动下一个taskset的程序
                    TaskSet taskSet = XxlJobAdminConfig.getAdminConfig().getTaskSetMapper().selectByPrimaryKey(xxlJobInfo.getTaskSetId());
                    String[] jobIdList = taskSet.getJobId().split(",");
                    int ifFinished = 1;
                    for(String s:jobIdList){
                        // 不考虑自己的执行状态
                        if(xxlJobInfo.getId() != Integer.valueOf(s)){
                            // 拿到了正在执行的日志
                            XxlJobLog finishJob = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().findFinishJob(Integer.valueOf(s));
                            if(ObjectUtils.isEmpty(finishJob) || finishJob.getHandleCode() != 200){
                                ifFinished = 0;
                                break;
                            }
                        }
                    }
                    if(ifFinished == 1){
                        // 证明本轮任务都完成了，需要触发下一轮操作
                        TaskSet nextTaskSet = XxlJobAdminConfig.getAdminConfig().getTaskSetMapper().selectByPrimaryKey(taskSet.getNextId());
                        jobIds = ObjectUtils.isEmpty(nextTaskSet)?null:nextTaskSet.getJobId();
                    }
                }
                // 再触发下游依赖任务，并执行分布式锁解锁
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
        if(null != lock){
            lock.unlock();
        }
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
