package com.cinema.ticketing.filter;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Enumeration;

/**
 * Feign 请求拦截器 - 自动传递请求头（包括 Authorization）
 */
@Slf4j
@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            // 传递所有请求头（包括 Authorization）
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    String value = request.getHeader(name);
                    
                    // 排除一些不需要传递的头
                    if (!"content-length".equalsIgnoreCase(name) && 
                        !"host".equalsIgnoreCase(name)) {
                        template.header(name, value);
                        log.debug("Feign 传递请求头: {} = {}", name, value);
                    }
                }
            }
        }
    }
}