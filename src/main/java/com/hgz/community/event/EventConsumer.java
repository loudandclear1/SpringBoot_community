package com.hgz.community.event;


import com.alibaba.fastjson.JSONObject;
import com.hgz.community.entity.DiscussPost;
import com.hgz.community.entity.Event;
import com.hgz.community.entity.Message;
import com.hgz.community.handler.NotificationHandler;
import com.hgz.community.service.DiscussPostService;
import com.hgz.community.service.ElasticsearchService;
import com.hgz.community.service.MessageService;
import com.hgz.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private NotificationHandler notificationHandler;

    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW, TOPIC_MESSAGE})
    public void handleCommentMessage(ConsumerRecord consumerRecord) {
        if (consumerRecord == null || consumerRecord.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        Event event = JSONObject.parseObject(consumerRecord.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误！");
            return;
        }

        // 发送站内通知
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        if (!event.getData().isEmpty()) {
            content.putAll(event.getData());
        }

        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);

        // 发送 WebSocket 通知
        try {
            notificationHandler.notifyUser(String.valueOf(event.getEntityUserId()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord consumerRecord) {
        if (consumerRecord == null || consumerRecord.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        Event event = JSONObject.parseObject(consumerRecord.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息的格式错误！");
            return;
        }

        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);
    }
}
