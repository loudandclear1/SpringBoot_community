package com.hgz.community.controller;

import com.hgz.community.entity.Event;
import com.hgz.community.entity.User;
import com.hgz.community.event.EventProducer;
import com.hgz.community.service.LikeService;
import com.hgz.community.util.CommunityConstant;
import com.hgz.community.util.CommunityUtil;
import com.hgz.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements CommunityConstant {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId, int postId) {
        User user = hostHolder.getUser();
        if(user == null) {
            return CommunityUtil.getJSONString(403, "你还没有登录哦！");
        }
        // 点赞
        likeService.like(user.getId(), entityType, entityId, entityUserId);

        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);

        // 点赞状态
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

        // 点赞结果
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        // 点赞事件
        if(likeStatus == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(user.getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);
            eventProducer.fireEvent(event);
        }

        return CommunityUtil.getJSONString(0, null, map);
    }
}
