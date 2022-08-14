package com.xxl.job.dp.common.refresher;

import com.alibaba.fastjson.JSON;
import com.xxl.job.dp.common.property.DtpProperties;
import com.xxl.job.dp.common.utils.PropertiesBinder;
import com.xxl.job.dp.core.DtpRegistry;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * AbstractRefresher related
 *
 * @author: yanhom
 * @since 1.0.0
 **/
@Slf4j
public abstract class AbstractRefresher implements Refresher {

    @Override
    public void refresh(String content) {

        if (StringUtils.isEmpty(content)) {
            log.warn("DynamicTp refresh, empty content or null fileType.");
            return;
        }

        try {
            val properties = doParse(content);
            doRefresh(properties);
        } catch (IOException e) {
            log.error("DynamicTp refresh error, content: {}, fileType: {}", content, e);
        }
    }

    public Map<Object, Object> doParse(String content) throws IOException {
        if (StringUtils.isEmpty(content)) {
            return Collections.emptyMap();
        }
        Properties properties = new Properties();
        properties.load(new StringReader(content));
        return properties;
    }

    // 从zookeeper中获取的结果只是properties文件形式那种key value，需要经过转换变成DtpProperties的形式
    protected void doRefresh(Map<Object, Object> properties) {
        if (CollectionUtils.isEmpty(properties)) {
            log.warn("DynamicTp refresh, empty properties.");
            return;
        }
        DtpProperties res = (DtpProperties) PropertiesBinder.bindDtpProperties(properties, new DtpProperties());
        DtpRegistry.refresh(res);
        // 刷新配置信息
        ZookeeperRefresher.refreshDtpProperties(res);
    }

    protected void doRefresh(DtpProperties dtpProperties) {
        DtpRegistry.refresh(dtpProperties);
    }

}
