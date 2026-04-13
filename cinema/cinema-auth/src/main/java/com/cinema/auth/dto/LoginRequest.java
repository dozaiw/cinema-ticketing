package com.cinema.auth.dto;

import lombok.Data;

/**
 * 登录请求参数DTO
 */
@Data
public class LoginRequest {
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 密手机号码，唯一标识
     */
    private String phone;
}