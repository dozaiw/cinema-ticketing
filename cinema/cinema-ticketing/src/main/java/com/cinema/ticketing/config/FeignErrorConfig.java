package com.cinema.ticketing.config;


import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

/**
 * Feign 错误解码器配置
 * 用于处理非 2xx 响应码，避免抛出异常
 */
// @Configuration  // 注释掉，不作为全局配置
public class FeignErrorConfig {
    
    @Bean
    public ErrorDecoder errorDecoder() {
     return new CustomErrorDecoder();
    }
    
    /**
     * 自定义错误解码器
     */
    public static class CustomErrorDecoder implements ErrorDecoder {
        
        @Override
        public Exception decode(String methodKey, Response response) {
            // 返回 null 表示不抛出异常，让调用方正常处理响应体
            // 这样即使返回 4xx/5xx 状态码，也不会抛出 FeignException
         return null;
        }
    }
}
