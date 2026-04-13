package com.cinema.ticketing.config;


import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

/**
 * 沙箱账户 Feign 客户端专用配置
 * 只影响 SandBoxAccountFeignClient
 */
public class SandBoxFeignConfig {
    
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
          return null;
        }
    }
}
