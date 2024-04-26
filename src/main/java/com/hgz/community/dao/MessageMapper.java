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
}
