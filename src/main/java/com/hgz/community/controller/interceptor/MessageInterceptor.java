package com.hgz.community.controller.interceptor;

import com.hgz.community.entity.User;
import com.hgz.community.service.MessageService;
import com.hgz.community.util.CommunityConstant;
import com.hgz.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MessageInterceptor implements HandlerInterceptor, CommunityConstant {

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private MessageService messageService;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
            int noticeUnreadCount = 0;
            noticeUnreadCount += messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            noticeUnreadCount += messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            noticeUnreadCount += messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            modelAndView.addObject("allUnreadCount", letterUnreadCount + noticeUnreadCount);
        }
    }
}
