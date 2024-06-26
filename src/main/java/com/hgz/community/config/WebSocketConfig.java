package com.hgz.community.config;

import com.hgz.community.controller.interceptor.TicketHandshakeInterceptor;
import com.hgz.community.handler.NotificationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private NotificationHandler notificationHandler;

    @Autowired
    private TicketHandshakeInterceptor ticketHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationHandler, "/ws/notify").setAllowedOrigins("*")
                .addInterceptors(ticketHandshakeInterceptor);
    }
}
