package com.xxl.job.delay.utils;

import com.xxl.job.delay.common.RedisQueueKeys;

/**
 * @Description
 * @Author shirenchuang
 * @Date 2019/7/31 10:18 AM
 **/
public class RedisKeyUtil {

    /**
     * 获取RD_LIST_TOPIC 的Key前缀
     * @return
     */
    public static String getTopicListPreKey(){
        return RedisQueueKeys.RD_LIST_TOPIC_PRE;
    }

    /**
     * 获取RD_LIST_TOPIC某个TOPIC的 Key
     * @param topic
     * @return
     */
    public static String getTopicListKey(String topic){
        return getTopicListPreKey().concat(topic);
    }

    /**
     * 从member中解析出TopicList的key
     * @param member
     * @return
     */
    public static String getTopicListKeyByMember(String member){
        return RedisKeyUtil.getTopicListKey(RedisKeyUtil.getTopicKeyBymember(member));
    }

    /**
     * 拼接TOPIC:ID
     * @param topic
     * @return
     */
    public static String getTopicId(String topic,String id){
        return topic.concat(":").concat(id);
    }



    /**
     * 获取所有Job数据存放的Hash_Table 的Key
     * @return
     */
    public static String getDelayQueueTableKey(){
        return RedisQueueKeys.REDIS_DELAY_TABLE;
    }

    /**
     * 获取延迟列表 ZSet的Key
     * @return
     */
    public static String getBucketKey(){
        return RedisQueueKeys.RD_ZSET_BUCKET_PRE;
    }


    /**
     * 根据member获取Topic
     * @param member
     * @return
     */
    public static String getTopicKeyBymember(String member){
        String[] s = member.split(":");
        return s[0];
    }

}
