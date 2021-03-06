package com.centit.support.common;

public interface ICachedObject<T> {
    //单位秒，表示一个月
    long NOT_REFRESH_PERIOD = 2592000L;
    //单位秒， 表示需要快速刷新
    long KEEP_FRESH_PERIOD = 60L;

    void evictCahce();

    //获取原始缓存数据
    T getRawTarget();
}
