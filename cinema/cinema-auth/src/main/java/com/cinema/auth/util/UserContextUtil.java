package com.cinema.auth.util;

import com.cinema.auth.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 用户上下文工具类 - 统一获取当前用户信息
 */
@Slf4j
@Component
public class UserContextUtil {

    /**
     * 获取当前认证的 Authentication
     */
    public Authentication getAuthentication() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null) {
            return null;
        }
        return context.getAuthentication();
    }

    /**
     * 获取当前用户（UserDetails）
     */
    public User getCurrentUser() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("当前用户未认证");
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        return null;
    }

    /**
     * 获取当前用户 ID
     */
    public Integer getUserId() {
        User user = getCurrentUser();
        if (user != null) {
            return user.getId();
        }
        throw new RuntimeException("用户未登录或用户信息不存在");
    }

    /**
     * 获取当前用户名
     */
    public String getUsername() {
        User user = getCurrentUser();
        if (user != null) {
            return user.getUsername();
        }
        throw new RuntimeException("用户未登录或用户信息不存在");
    }

    /**
     * 获取当前用户角色
     */
    public Integer getRole() {
        User user = getCurrentUser();
        if (user != null) {
            return user.getRole();
        }
        throw new RuntimeException("用户未登录或用户信息不存在");
    }

    /**
     * 判断用户是否已登录
     */
    public boolean isAuthenticated() {
        Authentication authentication = getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}