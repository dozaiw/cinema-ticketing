package com.cinema.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;

/**
 * 沙盒账户表(SandboxAccount)实体类
 *
 * @author makejava
 * @since 2026-03-10 15:15:15
 */
public class SandboxAccount implements Serializable {
    private static final long serialVersionUID = -53631928040831678L;
/**
     * 用户ID
     */
    @TableId
    private Integer userId;
/**
     * 虚拟余额
     */
    private Double balance;
/**
     * 支付密码（加密存储）
     */
    private String payPassword;


    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public String getPayPassword() {
        return payPassword;
    }

    public void setPayPassword(String payPassword) {
        this.payPassword = payPassword;
    }

}

