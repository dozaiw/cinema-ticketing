package com.cinema.auth.controller;

import com.cinema.auth.services.SandboxAccountService;
import com.cinema.auth.util.UserContextUtil;
import com.cinema.common.entity.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sandBoxAccount")
@Slf4j
public class SandboxAccountController {
    @Autowired
    private SandboxAccountService sandboxAccountService;
    @Autowired
    private UserContextUtil userContextUtil;

    //查询账户余额
    @GetMapping("/getSandboxAccount")
    public BaseResponse getSandboxAccount() {
        try {
            Integer userId = userContextUtil.getUserId();
            sandboxAccountService.getBalance(userId);
            return BaseResponse.success("查询成功", sandboxAccountService.getBalance(userId));
        }catch (Exception e){
            return BaseResponse.error(405,"余额查询失败");
        }
    }

    //账户扣款
    @PostMapping("/deductBalance")
    public BaseResponse deductBalance(@RequestParam(value = "amount") Double amount,@RequestParam(value = "passWord") String passWord) {
        Integer userId = userContextUtil.getUserId();
        BaseResponse response = sandboxAccountService.deductBalance(userId, amount, passWord);
        if (response.getCode() != 200) {
           return response;
        }
       return BaseResponse.success("扣款成功");
    }
    // 退款增加余额
    @PostMapping("/refundBalance")
    public BaseResponse refundBalance(@RequestParam(value = "amount")Double amount) {
        try {
            Integer userId = userContextUtil.getUserId();
            return sandboxAccountService.refundBalance(userId, amount);
        }catch (Exception e){
            return BaseResponse.error(405,"余额退款失败");
        }
    }
}
