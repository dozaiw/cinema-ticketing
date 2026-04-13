package com.cinema.hall.component;

import com.cinema.hall.component.SeatUpdateListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SeatWebSocketHandler extends TextWebSocketHandler {

    // 存储每个 scheduleId 的所有会话
    private static final ConcurrentHashMap<Integer, ConcurrentHashMap<String, WebSocketSession>> scheduleSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("========== WebSocket 尝试连接 ==========");
        log.info("URI: {}", session.getUri());
        log.info("Path: {}", session.getUri().getPath());
        log.info("Query: {}", session.getUri().getQuery());
        log.info("Headers: {}", session.getHandshakeHeaders());
        log.info("Session ID: {}", session.getId());

        try {
            String path = session.getUri().getPath();
            // 解析 URL：ws://.../ws/seat/{scheduleId}
            String[] paths = path.split("/");
            Integer scheduleId = Integer.parseInt(paths[paths.length - 1]);

            log.info("WebSocket 连接建立: scheduleId={}, sessionId={}", scheduleId, session.getId());

            scheduleSessions.computeIfAbsent(scheduleId, k -> new ConcurrentHashMap<>())
                    .put(session.getId(), session);
        } catch (Exception e) {
            log.error("WebSocket 连接建立失败", e);
            throw e;
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 从所有 scheduleId 的会话中移除
        scheduleSessions.values().forEach(sessions -> sessions.remove(session.getId()));
        log.info("WebSocket 连接关闭: sessionId={}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 传输错误: sessionId={}", session.getId(), exception);
        session.close();
    }

    // 发送座位更新消息到所有监听该 scheduleId 的客户端
    public void broadcastSeatUpdate(Integer scheduleId, SeatUpdateListener.SeatUpdateMessage message) {
        ConcurrentHashMap<String, WebSocketSession> sessions = scheduleSessions.get(scheduleId);
        if (sessions != null) {
            String json = convertToJson(message);
            sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(json));
                    }
                } catch (Exception e) {
                    log.error("发送 WebSocket 消息失败", e);
                }
            });
        }
    }

    private String convertToJson(SeatUpdateListener.SeatUpdateMessage message) {
        return String.format(
                "{\"scheduleId\":%d,\"seatScheduleId\":%d,\"eventType\":\"%s\",\"userId\":\"%s\",\"seatStatus\":%d}",
                message.getScheduleId(),
                message.getSeatScheduleId(),
                message.getEventType(),
                message.getUserId(),
                message.getSeatStatus()
        );
    }
}
