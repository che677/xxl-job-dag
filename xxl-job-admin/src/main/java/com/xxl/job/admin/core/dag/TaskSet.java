package com.xxl.job.admin.core.dag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskSet implements Comparable<TaskSet>{
    int id;
    // 如果里面所有的node都返回成功状态，则表示stage为成功
    List<Integer> nodeList = new ArrayList<>();

    // 成功，未成功（失败，运行中，等待执行在这里不考虑，默认可以成功，可以向后执行）
    boolean status = false;
    // 执行顺序（入队列的顺序）
    Integer sequence;

//    private final String TAG = getClass().getSimpleName();
//    protected WeakReference<BlockTaskQueue> taskQueue;//阻塞队列，存入实际要执行的任务集
//    //此队列用来实现任务时间不确定的队列阻塞功能
//    private PriorityBlockingQueue<Integer> blockQueue;

//    /**
//     * 初始化的时候，就把任务队列和阻塞队列生成了
//     *
//     */
//    public TaskSet() {
//        taskQueue = new WeakReference<>(new BlockTaskQueue());
//        blockQueue = new PriorityBlockingQueue<>();
//    }

//    /**
//     * 获取任务集的状态（所有节点都执行成功才算成功）
//     * @return
//     */
//    public boolean getStatus() {
//        for(Node r:nodeList){
//            if(!StatusTypeEnum.FINISHED.name().equals(r.getStatus()))
//                setStatus(false);
//        }
//        return status;
//    }
//
//    public void addNode(Node node){
//        nodeList.add(node);
//    }
//
//    public void removeNode(Long nodeId){
//        int i = 0;
//        Iterator<Node> iter = nodeList.iterator();
//        while(iter.hasNext()){
//            Node next = iter.next();
//            if(next.getId() == nodeId){
//                iter.remove();
//                return;
//            }
//        }
//    }
//
//    /**
//     * 执行任务完毕，剔除任务集，释放阻塞队列，修改执行状态
//     */
////    public void finishTask(){
////        this.status = false;
////        this.taskQueue.get().remove(this);
////        CurrentRunningTask.removeCurrentShowingTask();
////        System.out.println(TAG + taskQueue.get().size());
////    }
//
//    public void doProcess(ExecutorService threadPool) throws ExecutionException, InterruptedException {
//        setStatus(true);
//        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
//        // 利用completable，如果所有node线程的任务执行成功，才向后执行
//        List<Node> nodeList = getNodeList();
//                nodeList.forEach(r -> {
//            CompletableFuture<Boolean> future = CompletableFuture.
//                    supplyAsync(() -> {
//                        boolean res = false;
//                        try {
//                            res = r.doProcess();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                        return res;
//                    }, threadPool);
//            futures.add(future);
//        });
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
//                .join();
//        for (CompletableFuture<Boolean> r:futures) {
//            if(!r.get()){
//                setStatus(false);
//            }
//        }
//        if(getStatus()){
//            setStatus(true);
//            System.out.println(getStatus() + "执行成功");
//        }
//    }
//
//    // 必须要重构比较方法，否则在优先队列中，无法判断先拿出来哪一个任务集
//    // 这里是按照sequence来判断哪个任务集优先
    @Override
    public int compareTo(TaskSet another) {
        return this.getSequence() - another.getSequence();
    }

}
