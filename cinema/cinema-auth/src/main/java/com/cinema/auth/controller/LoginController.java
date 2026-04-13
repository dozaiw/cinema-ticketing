package com.cinema.auth.controller;

import com.cinema.auth.config.CosConfig;
import com.cinema.auth.dto.LoginRequest;
import com.cinema.auth.entity.User;
import com.cinema.auth.services.SandboxAccountService;
import com.cinema.auth.services.UserService;
import com.cinema.auth.util.CosUtil;
import com.cinema.auth.util.JwtUtil;
import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
@Slf4j
public class LoginController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CosUtil cosUtilAuth;

    @Autowired
    private CosConfig cosConfig;

    @Autowired
    private ObjectMapper objectMapper;


    @PostMapping("/login")
    public BaseResponse login(@RequestBody LoginRequest request){
        if(request.getUsername() == null || request.getPassword() == null){
            return BaseResponse.error(ResultCode.PARAM_ERROR);
        }
        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
            );
            Authentication authentication = authenticationManager.authenticate(authToken);
            User user = (User) authentication.getPrincipal();
            String token = jwtUtil.generateToken(user);
            String redisKey = "user:token:" + user.getId();
            redisTemplate.opsForValue().set(redisKey, token, jwtUtil.getExpireTime(), TimeUnit.MILLISECONDS);
            log.info("用户{}登录成功，生成 Token", request.getUsername());
            return BaseResponse.success(ResultCode.LOGIN_SUCCESS.getMsg(), token);
        } catch (DisabledException e) {
            log.error("用户{}登录失败：账号已禁用", request.getUsername(), e);
            return BaseResponse.error(ResultCode.USER_DISABLED);
        } catch (BadCredentialsException e) {
            log.error("用户{}登录失败：密码错误", request.getUsername(), e);
            return BaseResponse.error(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        } catch (UsernameNotFoundException e) {
            log.error("用户{}登录失败：用户不存在", request.getUsername(), e);
            return BaseResponse.error(ResultCode.USER_NOT_EXIST);
        } catch (Exception e) {
            log.error("用户{}登录失败：系统异常", request.getUsername(), e);
            return BaseResponse.error(ResultCode.SERVER_ERROR);
        }
    }

    @PostMapping("/regist")
    public BaseResponse regist(
            @RequestPart("user") String userJson,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {

        try {
            User user = objectMapper.readValue(userJson, User.class);

            // 基础验证
            if (user.getPhone() == null || user.getPassword() == null) {
                return BaseResponse.error(ResultCode.PARAM_ERROR);
            }

            // 检查手机号是否已注册
            User existingUser = userService.findByPhone(user.getPhone());
            if (existingUser != null) {
                return BaseResponse.error(400, "该手机号已注册，请直接登录");
            }

            // 设置用户名
            if (user.getUsername() == null) {
                user.setUsername(user.getPhone());
            }

            // 处理头像
            String avatarUrl;
            if (avatarFile != null && !avatarFile.isEmpty()) {
                avatarUrl = cosUtilAuth.uploadFile(avatarFile, "user/avatar");
            } else {
                avatarUrl = cosConfig.getDefaultAvatarUrl();
            }
            user.setAvatar(avatarUrl);

            // 设置默认角色和状态
            if (user.getRole() == null) user.setRole(1);
            if (user.getStatus() == null) user.setStatus(1);

            return userService.regist(user);

        } catch (Exception e) {
            log.error("注册失败", e);
            //  捕获唯一键冲突异常，返回友好提示
            if (e instanceof org.springframework.dao.DuplicateKeyException) {
                return BaseResponse.error(400, "该手机号已注册，请直接登录");
            }
            return BaseResponse.error(ResultCode.USER_REGISTER_FAILED);
        }
    }


    @PostMapping("/logout")
    public BaseResponse logout(HttpServletRequest request) {
        // ... 保持不变 ...
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return BaseResponse.error(ResultCode.TOKEN_INVALID);
        }
        String token = authHeader.substring(7);
        try {
            Claims claims = jwtUtil.parseToken(token);
            Integer userId = claims.get("userId", Integer.class);
            if (userId == null) {
                return BaseResponse.error(ResultCode.TOKEN_INVALID);
            }
            String redisKey = "user:token:" + userId;
            Boolean deleteResult = redisTemplate.delete(redisKey);
            if (deleteResult) {
                log.info("用户{}登出成功，Token 已删除", userId);
                return BaseResponse.success("登出成功");
            } else {
                return BaseResponse.error(ResultCode.TOKEN_EXPIRED);
            }
        } catch (Exception e) {
            log.error("登出失败：Token 解析异常", e);
            return BaseResponse.error(ResultCode.TOKEN_INVALID);
        }
    }

}