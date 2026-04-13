package com.cinema.auth.util;

import com.cinema.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
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
     * 生成Token
     */
    public  String generateToken(User user) {
        // 1. 生成符合HS256要求的秘钥
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        // 2. 构造JWT载荷（存储用户非敏感信息）
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId()); // 假设User有id字段
        claims.put("username", user.getUsername()); // 假设User有username字段
        claims.put("role", user.getRole()); // 假设User有role字段（权限控制）

        // 3. 构建并生成Token
        return Jwts.builder()
                .setClaims(claims) // 修复：传入载荷Map
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expireTime)) // 过期时间
                .signWith(key, SignatureAlgorithm.HS256) // 签名算法+秘钥
                .compact();
    }

    /**
     * 验证Token有效性
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claims = parseToken(token);
            // 校验2点：1. Token未过期  2. Token中的用户名和数据库一致
            String username = claims.get("username", String.class);
            return username.equals(userDetails.getUsername())
                    && !claims.getExpiration().before(new Date());
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
     * 从Token中获取用户ID
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