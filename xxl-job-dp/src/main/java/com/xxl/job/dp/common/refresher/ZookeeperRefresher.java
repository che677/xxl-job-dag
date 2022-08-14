package com.xxl.job.dp.common.refresher;

import com.xxl.job.dp.common.property.DtpProperties;
import com.xxl.job.dp.common.utils.CuratorUtil;
import com.xxl.job.dp.core.DtpMonitor;
import com.xxl.job.dp.core.DtpRegistry;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.util.Map;

/**
 * @author Redick01
 */
@Slf4j
public class ZookeeperRefresher extends AbstractRefresher implements SmartInitializingSingleton {

    public static void refreshDtpProperties(DtpProperties dtpProperties) {
        DtpRegistry.getDtpProperties().setExecutors(dtpProperties.getExecutors());
    }

//    public static DtpProperties getDtpProperties(){
//        return dtpProperties;
//    }

    @SneakyThrows
    @Override
    public void afterSingletonsInstantiated() {
        log.info("zookeeper连接初始化");
        // 断线之后，需要自己重新注册，并全量拉取数据
        final ConnectionStateListener connectionStateListener = (client, newState) -> {
            if (newState == ConnectionState.RECONNECTED) {
                loadAndRefresh();
            }};

        // 根据zk的地址获取一个连接
        CuratorFramework curatorFramework = CuratorUtil.getCuratorFramework(DtpRegistry.getDtpProperties());
        // 获取节点的根目录，也就是    /configserver/dev/项目名称
        String nodePath = CuratorUtil.nodePath(DtpRegistry.getDtpProperties());
        // 本来应该是利用配置中心的参数来初始化，但是这里为了方便，先利用yml的参数初始化，再修改参数与zk保持一致
        Map<Object, Object> objectObjectMap = CuratorUtil.genPropertiesMap(DtpRegistry.getDtpProperties());
        doRefresh(objectObjectMap);
        PathChildrenCacheListener cacheListener = new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                log.info("触发监听器:  "+new String(event.getData().getData()));
                switch (event.getType()) {
                    // 只有修改参数，才会触发refresh操作
                    case CHILD_UPDATED:
                        loadAndRefresh();
                        break;
                    default:
                        break;
                }
            }
        };

        // 添加state监听器，监听zk的状态，一旦断线之后再次连接成功，需要刷新状态
        curatorFramework.getConnectionStateListenable().addListener(connectionStateListener);
        PathChildrenCache curatorCache = CuratorUtil.getCuratorCache(nodePath);
        curatorCache.getListenable().addListener(cacheListener);
        curatorCache.start();
        log.info("DynamicTp refresher, add listener success, nodePath: {}", nodePath);
    }

    /**
     * load config and refresh
     */
    public void loadAndRefresh() {
        // 本类中的配置信息从来没有修改过，只有线程池中的参数才会被修改
        Map<Object, Object> objectObjectMap = CuratorUtil.genPropertiesMap(DtpRegistry.getDtpProperties());
        log.info("更新数据"+objectObjectMap);
        doRefresh(objectObjectMap);
    }

}
