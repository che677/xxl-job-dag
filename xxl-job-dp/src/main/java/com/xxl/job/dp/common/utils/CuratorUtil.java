package com.xxl.job.dp.common.utils;

import com.google.common.collect.Maps;
import com.xxl.job.dp.common.property.DtpProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * CuratorUtil related
 *
 * @author: yanhom
 * @since 1.0.4
 **/
@Slf4j
public class CuratorUtil {

    private static CuratorFramework curatorFramework;

    private static final CountDownLatch COUNT_DOWN_LATCH = new CountDownLatch(1);

    private CuratorUtil() {}

    public static CuratorFramework getCuratorFramework(DtpProperties dtpProperties) {
        if (curatorFramework == null) {
            // 获取zk地址
            DtpProperties.Zookeeper zookeeper = dtpProperties.getZookeeper();
            curatorFramework = CuratorFrameworkFactory.newClient(zookeeper.getZkConnectStr(),
                    new ExponentialBackoffRetry(1000, 3));
            final ConnectionStateListener connectionStateListener = (client, newState) -> {
                if (newState == ConnectionState.CONNECTED) {
                    log.info("连接zk集群成功");
                    COUNT_DOWN_LATCH.countDown();
                }};
            curatorFramework.getConnectionStateListenable().addListener(connectionStateListener);
            curatorFramework.start();
            try {
                COUNT_DOWN_LATCH.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
        return curatorFramework;
    }

    public static PathChildrenCache getCuratorCache(String path){
        if (curatorFramework == null) {
            log.error("请先初始化zk客户端");
            return null;
        }
        PathChildrenCache curatorCache = new PathChildrenCache(curatorFramework, path,true);
        return curatorCache;
    }

    public static String nodePath(DtpProperties dtpProperties) {
        DtpProperties.Zookeeper zookeeper = dtpProperties.getZookeeper();
        return ZKPaths.makePath(ZKPaths.makePath(zookeeper.getRootNode(),
                zookeeper.getConfigVersion()), zookeeper.getNode());
    }

    @SneakyThrows
    public static Map<Object, Object> genPropertiesMap(DtpProperties dtpProperties) {
        val curatorFramework = getCuratorFramework(dtpProperties);
        String nodePath = nodePath(dtpProperties);
        Map<Object, Object> result = Maps.newHashMap();
        result = genPropertiesTypeMap(nodePath, curatorFramework);
        return result;
    }

    private static Map<Object, Object> genPropertiesTypeMap(String nodePath, CuratorFramework curatorFramework) {
        try {
            final GetChildrenBuilder childrenBuilder = curatorFramework.getChildren();
            final List<String> children = childrenBuilder.watched().forPath(nodePath);
            Map<Object, Object> properties = Maps.newHashMap();
            children.forEach(c -> {
                String path = ZKPaths.makePath(nodePath, c);
                final String nodeName = ZKPaths.getNodeFromPath(path);
                String value = getVal(path, curatorFramework);
                properties.put(nodeName, value);
            });
            return properties;
        } catch (Exception e) {
            log.error("get zk configs error, nodePath is {}", nodePath, e);
            return Collections.emptyMap();
        }
    }

    private static String getVal(String path, CuratorFramework curatorFramework) {
        final GetDataBuilder data = curatorFramework.getData();
        String value = "";
        try {
            value = new String(data.watched().forPath(path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("get zk config value failed, path: {}", path, e);
        }
        return value;
    }
}
