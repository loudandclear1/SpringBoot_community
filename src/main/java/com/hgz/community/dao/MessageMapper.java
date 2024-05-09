package com.hgz.community.dao;

import com.hgz.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper {

    // 会话
    List<Message> selectConversations(int userId, int offset, int limit);

    // 会话数量
    int selectConversationCount(int userId);

    // 会话中的私信
    List<Message> selectLetters(String conversationId, int offset, int limit);

    // 私信数量
    int selectLetterCount(String conversationId);

    // 未读私信数量
    int selectLetterUnreadCount(int userId, String conversationId);

    // 添加消息
    int insertMessage(Message message);

    // 修改消息状态
    int updateStatus(List<Integer> ids, int status);

    // 查询某个主题下最新通知
    Message selectLatestNotice(int userId, String topic);

    // 查询某个主题所包含的通知数量
    int selectNoticeCount(int userId, String topic);

    // 查询未读的通知数量
    int selectNoticeUnreadCount(int userId, String topic);

    // 查询某个主题所包含的通知列表
    List<Message> selectNotices(int userId, String topic, int offset, int limit);
}
