package com.cinema.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

/**
 * 用户表(User)实体类
 *
 * @author makejava
 * @since 2026-01-11 15:45:14
 */
public class User implements Serializable, UserDetails { // 关键：实现UserDetails接口
    private static final long serialVersionUID = 394663256708785713L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 角色
     */
    private Integer role;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 账号是否启用（补充缺失字段）
     */
    private Integer status;



    // ========== 原有getter/setter方法 ==========
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    // ========== UserDetails接口实现方法 ==========
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String authority;
        // 数字角色映射逻辑
        if (role == 0) {
            authority = "admin"; // 0=管理员→admin权限
        } else if (role == 1) {
            authority = "user"; // 1=用户→user权限
        } else {
            // 未知角色：返回空权限（或自定义默认权限）
            return Collections.emptyList();
        }
        // 返回Spring Security可识别的权限对象
        return Collections.singletonList(new SimpleGrantedAuthority(authority));
    }

    @Override
    public boolean isAccountNonExpired() {
        // 业务需求：默认账号永不过期，可根据实际场景修改（如从数据库读取过期状态）
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 业务需求：默认账号不锁定，可扩展字段（如locked）控制
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 业务需求：默认凭证永不过期，可扩展字段（如credentialsExpired）控制
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}