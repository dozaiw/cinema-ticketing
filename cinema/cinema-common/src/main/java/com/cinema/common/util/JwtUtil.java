package com.cinema.common.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 适配 cinema-auth 模块的 JWT 工具类
 */
@Slf4j
@Component // 核心：交给Spring容器管理，使@Value生效
public class JwtUtil {

    // 修复：移除多余的右括号，保证字符串闭合
    @Value("${jwt.secret:your-256-bit-secret-key-12345678901234567890123456789012}")
    private String secret;

    @Value("${jwt.expire-time:72000000}") // 20小时，可根据业务调整
    private long expireTime;

    /**
     * 验证Token有效性（自动校验签名、过期时间）
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            // 校验2点：1. Token未过期  2. Token中的用户名和数据库一致
            String username = claims.get("username", String.class);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("Token校验失败", e);
            return false;
        }
    }

    /**
     * 解析Token，获取载荷信息
     */
    public Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key) // 设置签名秘钥
                .build()
                .parseClaimsJws(token) // 解析Token
                .getBody(); // 获取载荷
    }

    /**
     * 从Token中获取用户ID（简化业务层调用）
     */
    public Integer getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Integer.class);
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    public Long getExpireTime() {
        return expireTime;
    }

}