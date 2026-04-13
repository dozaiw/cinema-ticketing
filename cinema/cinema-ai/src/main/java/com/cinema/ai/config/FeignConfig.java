package com.cinema.ai.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 客户端配置
 * 自动传递 JWT Token 和其他请求头
 */
@Configuration
@Slf4j
public class FeignConfig {
    
    /**
     * 请求拦截器：自动添加 Authorization 请求头
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // 获取原始请求的 Authorization 头
                String authorization = request.getHeader("Authorization");
                
                // 如果存在且以 Bearer 开头，则添加到 Feign 请求中
                if (authorization != null && authorization.startsWith("Bearer ")) {
                    template.header("Authorization", authorization);
                    log.debug("Feign 拦截器：已传递 JWT Token");
                } else if (authorization != null) {
                    log.warn("Authorization 格式不正确：{}", authorization);
                } else {
                    log.debug("未找到 Authorization 头");
                }
                
                // 可选：传递其他需要的请求头
                String internalToken = request.getHeader("internal-token");
                if (internalToken != null) {
                    template.header("internal-token", internalToken);
                }
            } else {
                log.debug("无 HTTP 请求上下文（可能是定时任务或后台调用）");
            }
        };
    }
}
