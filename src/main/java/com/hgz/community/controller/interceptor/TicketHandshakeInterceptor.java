package com.hgz.community.controller.interceptor;

import com.hgz.community.entity.LoginTicket;
import com.hgz.community.entity.User;
import com.hgz.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import java.util.Date;
import java.util.Map;

@Configuration
public class TicketHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private UserService userService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            Cookie cookie = WebUtils.getCookie(servletRequest.getServletRequest(), "ticket");
            if (cookie != null) {
                String ticket = cookie.getValue();
                // 这里假设 findLoginTicket 是从数据库检索 ticket 并返回关联用户的方法
                LoginTicket loginTicket = userService.findLoginTicket(ticket);
                if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                    User user = userService.findUserById(loginTicket.getUserId());
                    if (user != null) {
                        attributes.put("userId", user.getId());
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}
