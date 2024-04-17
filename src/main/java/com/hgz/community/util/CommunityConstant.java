package com.hgz.community.util;

public interface CommunityConstant {
    // 激活成功
    int ACTIVATION_SUCCESS = 0;

    // 重复激活
    int ACTIVATION_REPEAT = 1;

    // 激活失败
    int ACTIVATION_FAILURE = 2;

    // 默认登录超时时间
    int DEFAULT_EXPIRED_SECONDS = 3600 * 24;

    // 记住我 登录超时时间
    int REMEMBER_EXPIRED_SECOND = 3600 * 24 * 14;
}
