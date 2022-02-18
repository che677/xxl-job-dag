package com.xxl.job.core.util.beanutil;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ListBeanUtils extends BeanUtils {

    public static <S, T> List<T> copyList(List<S> sources, Supplier<T> target){
        return copyListProperties(sources, target, null);
    }

    public static <S, T> List<T> copyListProperties(List<S> sources, Supplier<T> target,
                                                    BeanCopyUtilCallBack<S, T> callBack){
        List<T> lists = new ArrayList<>(sources.size());
        sources.forEach(s -> {
            T t = target.get();
            copyProperties(s, t);
            lists.add(t);
            if(null != callBack){
                callBack.callback(s, t);
            }
        });
        return lists;
    }

}
