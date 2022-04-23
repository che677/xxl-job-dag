package com.xxl.job.core.util;

import org.springframework.util.ObjectUtils;

public class StringUtils {

    public static String toString(Object s){
        if(ObjectUtils.isEmpty(s)){
            return null;
        }
        return String.valueOf(s);
    }

}
