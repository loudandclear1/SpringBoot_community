package com.hgz.community.config;

import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {
    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        // 确保所有到/ws/**的WebSocket通信都需要认证
        messages
                .simpMessageDestMatchers("/ws/**")
                .authenticated().anyMessage().authenticated();
    }

    // 不禁用CSRF保护
    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}
