package com.cinema.auth.services.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cinema.auth.entity.SandboxAccount;
import com.cinema.auth.mapper.SandboxAccountMapper;
import com.cinema.auth.services.SandboxAccountService;
import com.cinema.common.entity.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
public class SandboxAccountServiceImpl implements SandboxAccountService {
    @Autowired
    private SandboxAccountMapper sandboxAccountMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // 用户注册时创建初始资金账号信息
    @Override
    @Transactional
    public boolean createAccount(Integer userId) {
        SandboxAccount sandboxAccount = new SandboxAccount();
        sandboxAccount.setUserId(userId);
        sandboxAccount.setBalance(500.00);
        //密码加密
        String encryptedPassword = this.passwordEncoder.encode("123456");
        sandboxAccount.setPayPassword(encryptedPassword);  //密码默认123456
        try {
            sandboxAccountMapper.insert(sandboxAccount);
        } catch (Exception e) {
            log.error("创建资金账号失败", e);sandboxAccountMapper.update(new LambdaQueryWrapper<SandboxAccount>().eq(SandboxAccount::getUserId, userId));

            return false;
        }
        return true;
    }

    // 账号扣款
    @Override
    @Transactional
    public BaseResponse deductBalance(Integer userId, Double amount, String passWord) {
        SandboxAccount sandboxAccount = sandboxAccountMapper.selectOne(new LambdaQueryWrapper<SandboxAccount>().eq(SandboxAccount::getUserId, userId));
        if (sandboxAccount == null) {
            log.error("用户{}的账号不存在", userId);
            return BaseResponse.error(405,"账号不存在");
        }
        if(!this.passwordEncoder.matches(passWord, sandboxAccount.getPayPassword())){
            log.error("用户{}的支付密码错误", userId);
            return BaseResponse.error(405,"密码错误");
        }
        if (sandboxAccount.getBalance() < amount) {
            log.error("用户{}的余额不足", userId);
            return BaseResponse.error(406,"余额不足");
        }
        sandboxAccount.setBalance(sandboxAccount.getBalance() - amount);
        try {
            sandboxAccountMapper.updateById(sandboxAccount);
        } catch (Exception e) {
            log.error("用户{}的扣款失败", userId, e);
            return BaseResponse.error(407,"扣款失败");
        }
        return BaseResponse.success("扣款成功");
     }

     @Override
     @Transactional
    public Double getBalance(Integer userId) {
        SandboxAccount sandboxAccount = sandboxAccountMapper.selectOne(new LambdaQueryWrapper<SandboxAccount>().eq(SandboxAccount::getUserId, userId));
        if (sandboxAccount == null) {
            log.error("用户{}的账号不存在", userId);
            return null;
        }
        return sandboxAccount.getBalance();
    }

    //余额增加退款时使用
    @Override
    @Transactional
    public BaseResponse refundBalance(Integer userId, Double amount) {
        SandboxAccount sandboxAccount = sandboxAccountMapper.selectOne(new LambdaQueryWrapper<SandboxAccount>().eq(SandboxAccount::getUserId, userId));
        if (sandboxAccount == null) {
            log.error("用户{}的账号不存在", userId);
            return BaseResponse.error(405,"账号不存在");
        }
        sandboxAccount.setBalance(sandboxAccount.getBalance() + amount);
        try {
            sandboxAccountMapper.updateById(sandboxAccount);
        } catch (Exception e) {
            log.error("用户{}的退款失败", userId, e);
            return BaseResponse.error(407,"退款失败");
        }
        return BaseResponse.success("退款成功");
    }
}
