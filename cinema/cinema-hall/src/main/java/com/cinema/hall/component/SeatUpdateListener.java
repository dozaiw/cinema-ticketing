package com.cinema.hall.component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class SeatUpdateListener implements MessageListener {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SeatWebSocketHandler seatWebSocketHandler;  // 注入 WebSocket 处理器

    private static final Pattern PRESELECT_KEY_PATTERN =
            Pattern.compile("^seat:preselect:(\\d+):(\\d+)$");

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            var connection = redisConnectionFactory.getConnection();
            // 只订阅 seat:preselect:* 的 keyspace 事件
            connection.pSubscribe(
                    this,
                    "__keyspace@0__:seat:preselect:*".getBytes(),
                    "__keyevent@0__:set".getBytes(),
                    "__keyevent@0__:del".getBytes(),
                    "__keyevent@0__:expire".getBytes(),
                    "__keyevent@0__:expired".getBytes()
            );
            log.info("Redis Keyspace Listener 已启动 ");
        } catch (Exception e) {
            log.error("Redis Keyspace Listener 启动失败", e);
        }
    }


    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String key = new String(message.getBody());

            log.info("收到 Redis 事件 | channel: {} | key: {}", channel, key);

            if (!key.startsWith("seat:preselect:")) {
                return;
            }

            String eventType = extractEventType(channel, key);
            if (eventType == null) {
                log.warn(" 无法解析事件类型: {}", channel);
                return;
            }

            log.info(" 事件类型: {}", eventType);

            // 推送前验证：锁座记录必须存在
            if ("set".equals(eventType) || "expire".equals(eventType)) {
                String userId = stringRedisTemplate.opsForValue().get(key);

                if (userId == null || userId.isEmpty()) {
                    log.warn(" 锁座记录不存在，跳过推送: {}", key);
                    return;
                }
            }

            java.util.regex.Matcher matcher = PRESELECT_KEY_PATTERN.matcher(key);
            if (!matcher.matches()) {
                return;
            }

            Integer scheduleId = Integer.valueOf(matcher.group(1));
            Integer seatScheduleId = Integer.valueOf(matcher.group(2));

            log.info(" 座位变化: scheduleId={}, seatId={}, event={}",
                    scheduleId, seatScheduleId, eventType);

            SeatUpdateMessage msg = new SeatUpdateMessage();
            msg.setScheduleId(scheduleId);
            msg.setSeatScheduleId(seatScheduleId);
            msg.setEventType(eventType);

            if ("set".equals(eventType) || "expire".equals(eventType)) {
                String userId = stringRedisTemplate.opsForValue().get(key);
                msg.setUserId(userId);
                msg.setSeatStatus(1);
            } else {
                msg.setSeatStatus(0);
            }

            // 发送 STOMP 消息
            String topic = "/topic/seat/" + scheduleId;
            messagingTemplate.convertAndSend(topic, msg);

            // 发送 WebSocket 消息
            seatWebSocketHandler.broadcastSeatUpdate(scheduleId, msg);

            log.info(" WebSocket 推送: topic={}, seatId={}, status={}, userId={}",
                    topic, seatScheduleId, msg.getSeatStatus(), msg.getUserId());

        } catch (Exception e) {
            log.error("处理座位事件失败", e);
        }
    }

    private String extractEventType(String channel, String key) {
        if (channel.startsWith("__keyspace@")) {
            String[] parts = channel.split(":");
            return parts[parts.length - 1];
        } else if (channel.startsWith("__keyevent@")) {
            String[] parts = channel.split(":");
            String event = parts[parts.length - 1];
            if ("expired".equals(event) && key.startsWith("seat:preselect:")) {
                return "expired";
            }
            return event;
        }
        return null;
    }

    @Data
    public static class SeatUpdateMessage {
        private Integer scheduleId;
        private Integer seatScheduleId;
        private String eventType;
        private String userId;
        private Integer seatStatus;
    }
}
