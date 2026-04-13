// com.cinema.ai.controller.CinemaAgentController.java
package com.cinema.ai.controller;

import cn.hutool.json.JSONUtil;

import com.cinema.ai.dto.AgentChatRequest;
import com.cinema.ai.memory.ConversationMemory;
import com.cinema.auth.util.UserContextUtil;
import com.cinema.common.entity.BaseResponse;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai/agent")
@Slf4j
public class CinemaAgentController {

    @Resource
    private ChatClient cinemaAgentClient;
    
    @Resource
    private ConversationMemory conversationMemory;
    
    @Resource
    private UserContextUtil userContextUtil;

    /**
     *  智能客服对话接口
     */
    @PostMapping("/chat")
    public BaseResponse chat(@RequestBody AgentChatRequest request) {
        String sessionId = request.getSessionId();
        String userMessage = request.getMessage();
        
        log.info("收到 AI 请求：sessionId={}, message={}", sessionId, userMessage);
        
        try {
            //  1. 获取/保存当前用户信息
            Integer userId = null;
            try {
                userId = userContextUtil.getUserId();
                conversationMemory.saveSessionUserId(sessionId, userId);
                log.info("获取到用户 ID: {}", userId);
            } catch (Exception e) {
                // 未登录，从历史会话尝试恢复
                userId = conversationMemory.getSessionUserId(sessionId);
                log.info("未登录，从会话恢复用户 ID: {}", userId);
            }
            
            //  2. 记录用户输入
            conversationMemory.addMessage(sessionId, "user", userMessage);
            
            //  3. 构建用户上下文
            String userContext = buildUserContext(userId);
            
            //  4. 获取历史对话
            String history = conversationMemory.getHistoryContext(sessionId);
            String historyPrompt = history.isEmpty() ? "" : "【对话历史】\n" + history + "\n\n";
            
            //  5. 调用 Agent
            Map<String, Object> extraProps = new HashMap<>();
            extraProps.put("incremental_output", true);
            extraProps.put("enable_thinking", false);
            
            DashScopeChatOptions options = DashScopeChatOptions.builder()
                    .withModel("qwen3.5-flash")
                    .withTemperature(0.7)
                    .withMultiModel(true)
                    .build();
            
            // 通过反射设置 extraBody
            try {
                java.lang.reflect.Method setExtraBody = options.getClass().getDeclaredMethod("setExtraBody", Map.class);
                setExtraBody.setAccessible(true);
                setExtraBody.invoke(options, extraProps);
            } catch (Exception e) {
                log.warn("无法设置 extraBody", e);
            }
            
            String aiResponse = cinemaAgentClient.prompt()
                    .options(options)
                    .user("""
                        %s%s
                        【用户最新问题】
                        %s
                        """.formatted(userContext, historyPrompt, userMessage))
                    .call()
                    .content();
            
            //  6. 记录 AI 回复
            conversationMemory.addMessage(sessionId, "assistant", aiResponse);
            
            log.info("AI 对话成功，sessionId={}, userId={}", sessionId, userId);
            
            return BaseResponse.success("AI 回复", Map.of(
                "message", aiResponse,
                "sessionId", sessionId
            ));
            
        } catch (Exception e) {
            log.error("Agent 对话失败，sessionId={}", sessionId, e);
            return BaseResponse.error(500, "智能服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 构建用户上下文提示
     */
    private String buildUserContext(Integer userId) {
        if (userId != null) {
            return String.format("【当前用户】已登录，用户 ID: %d\n" +
                               "• 查询收藏时自动使用此 userId\n" +
                               "• 不要询问用户 ID，直接使用即可", userId);
        } else {
            return "【当前用户】未登录\n" +
                   "• 涉及收藏的操作需提示用户先登录\n" +
                   "• 可以正常搜索/推荐电影";
        }
    }

    /**
     * 清空对话历史
     */
    @PostMapping("/clear/{sessionId}")
    public BaseResponse clearHistory(@PathVariable("sessionId") String sessionId) {
        conversationMemory.clear(sessionId);
        return BaseResponse.success("对话已清空");
    }
}