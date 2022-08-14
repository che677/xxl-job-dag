package com.xxl.job.dp.common.utils;

import com.xxl.job.dp.common.property.DtpProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import java.util.Map;
import static com.xxl.job.dp.common.cons.DynamicTpConst.MAIN_PROPERTIES_PREFIX;

/**
 * PropertiesBinder related
 *
 * @author: yanhom
 * @since 1.0.3
 **/
public class PropertiesBinder {

    // 将key value形式的properties文件，转换成 DtpProperties 配置类
    public static Object bindDtpProperties(Map<?, Object> properties, DtpProperties dtpProperties) {
        ConfigurationPropertySource sources = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(sources);
        ResolvableType type = ResolvableType.forClass(DtpProperties.class);
        Bindable<?> target = Bindable.of(type).withExistingValue(dtpProperties);
        BindResult<?> bind = binder.bind(MAIN_PROPERTIES_PREFIX, target);
        return bind.get();
    }
}
