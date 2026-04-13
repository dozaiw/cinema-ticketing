// com.cinema.ai.dto.AgentChatRequest.java
package com.cinema.ai.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentChatRequest {
    private String sessionId;   // 会话 ID
    private String message;     // 用户消息
}