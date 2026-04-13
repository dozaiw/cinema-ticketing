package com.cinema.hall.config;

import com.cinema.hall.component.SeatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker  // 添加此注解启用 STOMP 支持
public class WebSocketConfig implements WebSocketConfigurer, WebSocketMessageBrokerConfigurer {

    @Autowired
    private SeatWebSocketHandler seatWebSocketHandler;

    // 配置消息代理
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");  // 启用简单消息代理
        config.setApplicationDestinationPrefixes("/app");  // 客户端发送消息的前缀
    }

    // 注册 STOMP 端点
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    // 注册原生 WebSocket 处理器
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(seatWebSocketHandler, "/ws/seat/{scheduleId}")
                .setAllowedOriginPatterns("*");
    }
}
