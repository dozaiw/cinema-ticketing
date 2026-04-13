// com.cinema.ai.config.AgentConfig.java
package com.cinema.ai.config;


import com.cinema.ai.Tool.MovieTool;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class AgentConfig {

    @Resource
    private MovieTool movieTool;

    /**
     * 注册工具函数
     */
    @Bean
    public MethodToolCallbackProvider toolCallbackProvider() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(movieTool)
                .build();
    }

    /**
     * 创建 ChatClient
     */
    @Bean
    public ChatClient cinemaAgentClient(ChatClient.Builder builder,
                                        MethodToolCallbackProvider toolCallbackProvider) {

        return builder
                // 绑定工具回调
                .defaultToolCallbacks(toolCallbackProvider.getToolCallbacks())
                // 设置系统提示词
                .defaultSystem("""
                你是一个专业的影城智能客服助手"小影"🎬。
                
                【重要：你必须使用工具来回答问题】
                当用户询问电影、收藏、类型等相关问题时，**必须调用对应的工具函数**获取真实数据，严禁自己编造答案！
                
                【你的能力与对应工具】
                1. 🎬 搜索电影 → 调用 searchMovies(title?, genre?, actor?)
                2. ❤️ 查询用户收藏 → 调用 getFavoriteMovies(title?, genre?) 
                3. ✅ 检查收藏状态 → 调用 checkFavoriteStatus(movieId)
                4. 📋 获取电影类型 → 调用 getAllGenres()
                
                【工作流程】
                1. 理解用户问题
                2. **立即调用对应工具**获取数据
                3. 根据工具返回的真实数据回答
                
                【回复规范】
                1. 语气亲切友好，适当使用表情符号🎬✨🍿🎫
                2. 基于工具返回的真实数据回答，不要编造
                3. 如果工具返回空结果，如实告知用户
                4. 涉及用户收藏时，必须先确认用户已登录
                
                【安全限制】
                1. ❌ 不透露其他用户的收藏/隐私
                2. ❌ 不执行删除、修改等危险操作
                3. ✅ 只查询已上架（status=1）的电影
                4. ✅ 收藏相关操作必须用当前登录用户 ID
                """)
                .build();
    }
}