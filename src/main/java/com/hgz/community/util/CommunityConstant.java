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

    // 实体类型：帖子
    int ENTITY_TYPE_POST = 1;

    // 实体类型：评论
    int ENTITY_TYPE_COMMENT = 2;

    // 实体类型：用户
    int ENTITY_TYPE_USER = 3;

    // 主题
    String TOPIC_COMMENT = "comment";

    // 主题
    String TOPIC_LIKE = "like";

    // 主题
    String TOPIC_FOLLOW = "follow";

    // 发布帖子
    String TOPIC_PUBLISH = "publish";

    // 删除帖子
    String TOPIC_DELETE = "delete";

    // 私信
    String TOPIC_LETTER = "letter";

    // 系统用户ID
    int SYSTEM_USER_ID = 1;

    // 权限：普通用户
    String AUTHORITY_USER = "user";

    // 权限：管理员
    String AUTHORITY_ADMIN = "admin";

    // 权限：版主
    String AUTHORITY_MODERATOR = "moderator";
}
