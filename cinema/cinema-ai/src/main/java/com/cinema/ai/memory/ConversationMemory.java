// com.cinema.ai.memory.ConversationMemory.java
package com.cinema.ai.memory;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;  // 🔑 改导入
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "rawtypes"})
@Component
@Slf4j
public class ConversationMemory {

    //  改为使用 RedisTemplate<String, Object>
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String MEMORY_PREFIX = "ai:conversation:";
    private static final String SESSION_USER_PREFIX = "ai:session:";
    private static final int MAX_TURNS = 10;
    private static final long EXPIRE_MINUTES = 30;

    /**
     * 添加对话记录（按用户 + sessionId 存储）
     */
    public void addMessage(String sessionId, String role, String content) {
        Integer userId = getSessionUserId(sessionId);
        String key = buildMemoryKey(userId, sessionId);

        // 使用 Map 存储结构化数据
        Map<String, Object> messageObj = new java.util.HashMap<>();
        messageObj.put("role", role);
        messageObj.put("content", content);
        messageObj.put("timestamp", System.currentTimeMillis());

        redisTemplate.opsForList().rightPush(key, messageObj);

        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > MAX_TURNS * 2) {
            redisTemplate.opsForList().leftPop(key);
        }

        redisTemplate.expire(key, EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        log.info(" 记忆已保存：userId={}, sessionId={}, role={}",
            userId != null ? userId : "anonymous", sessionId, role);
    }

    /**
     * 获取对话历史（根据用户 + sessionId）
     */
    public String getHistoryContext(String sessionId) {
        Integer userId = getSessionUserId(sessionId);
        String key = buildMemoryKey(userId, sessionId);
        
        //  使用通配符类型
        java.util.List<?> messages = redisTemplate.opsForList().range(key, 0, -1);

        if (messages == null || messages.isEmpty()) {
            return "";
        }

        return messages.stream()
                .map(this::parseMessage)
                .collect(Collectors.joining("\n"));
    }
    
    /**
     *  辅助方法：解析单条消息
     */
    @SuppressWarnings("unchecked")
    private String parseMessage(Object obj) {
        try {
            if (obj instanceof java.util.Map) {
                java.util.Map<String, Object> messageMap = (java.util.Map<String, Object>) obj;
                String role = (String) messageMap.get("role");
                String content = (String) messageMap.get("content");
                return String.format("%s: %s", role, content);
            } else {
                return obj != null ? obj.toString() : "";
            }
        } catch (Exception e) {
            log.warn("解析消息失败：{}", obj, e);
            return "解析失败";
        }
    }

    /**
     *  获取结构化的消息列表
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Map<String, Object>> getMessages(String sessionId) {
        Integer userId = getSessionUserId(sessionId);
        String key = buildMemoryKey(userId, sessionId);
        
        // 改为 List<Object>，然后逐个转换
        List<Object> messagesObj = redisTemplate.opsForList().range(key, 0, -1);

        if (messagesObj == null || messagesObj.isEmpty()) {
            return Collections.emptyList();
        }

        return messagesObj.stream()
                .map(obj -> {
                    try {
                        if (obj instanceof Map) {
                            Map<String, Object> messageMap = (Map<String, Object>) obj;
                            Map<String, Object> resultMap = new java.util.HashMap<>();
                            resultMap.put("role", messageMap.get("role"));
                            resultMap.put("content", messageMap.get("content"));
                            resultMap.put("timestamp", messageMap.get("timestamp"));
                            return resultMap;
                        } else {
                            // 不应该到这里，但做个容错处理
                            Map<String, Object> errorMap = new java.util.HashMap<>();
                            errorMap.put("role", "error");
                            errorMap.put("content", "非 Map 类型数据");
                            errorMap.put("timestamp", 0L);
                            return errorMap;
                        }
                    } catch (Exception e) {
                        log.error("解析消息失败", e);
                        Map<String, Object> errorMap = new java.util.HashMap<>();
                        errorMap.put("role", "error");
                        errorMap.put("content", "解析失败：" + e.getMessage());
                        errorMap.put("timestamp", 0L);
                        return errorMap;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 清空指定会话的历史
     */
    public void clear(String sessionId) {
        Integer userId = getSessionUserId(sessionId);
        String key = buildMemoryKey(userId, sessionId);
        redisTemplate.delete(key);
        log.info("🗑️ 会话已清空：userId={}, sessionId={}", userId, sessionId);
    }
    
    /**
     * 清空用户的所有会话历史
     */
    public void clearAllUserSessions(Integer userId) {
        String pattern = MEMORY_PREFIX + "user:" + userId + ":session:*";
        redisTemplate.delete(redisTemplate.keys(pattern));
        log.info("🗑️ 用户所有会话已清空：userId={}", userId);
    }

    /**
     * 获取用户的所有 sessionId 列表
     */
    public List<String> getUserSessionIds(Integer userId) {
        String pattern = MEMORY_PREFIX + "user:" + userId + ":session:*";
        Set<String> keys = redisTemplate.keys(pattern);
        
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 从 key 中提取 sessionId
        return keys.stream()
                .map(key -> key.replace(MEMORY_PREFIX + "user:" + userId + ":session:", ""))
                .collect(Collectors.toList());
    }

    /**
     * 获取会话用户 ID
     */
    public Integer getSessionUserId(String sessionId) {
        String key = SESSION_USER_PREFIX + sessionId;
        Object userId = redisTemplate.opsForValue().get(key);
        return userId != null ? Integer.valueOf(userId.toString()) : null;
    }

    /**
     * 保存会话用户 ID
     */
    public void saveSessionUserId(String sessionId, Integer userId) {
        String key = SESSION_USER_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, userId.toString(), EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.debug("会话用户绑定：sessionId={}, userId={}", sessionId, userId);
    }
    
    /**
     * 构建记忆存储的 key
     * 格式：// ... existing code ...

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer stringSerializerai:conversation:user:{userId}:session:{sessionId}
     */
    private String buildMemoryKey(Integer userId, String sessionId) {
        if (userId == null) {
            // 未登录用户：使用匿名前缀
            return MEMORY_PREFIX + "anonymous:session:" + sessionId;
        }
        return MEMORY_PREFIX + "user:" + userId + ":session:" + sessionId;
    }
}