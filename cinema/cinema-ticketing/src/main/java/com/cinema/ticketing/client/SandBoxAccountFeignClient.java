package com.cinema.ticketing.client;

import com.cinema.common.entity.BaseResponse;
import com.cinema.ticketing.config.SandBoxFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "cinema-auth", path = "/sandBoxAccount", configuration = SandBoxFeignConfig.class)
public interface SandBoxAccountFeignClient {
    
    /**
     * 查询账户余额
     * @return BaseResponse，包含余额数据
     */
    @GetMapping("/getSandboxAccount")
    BaseResponse getSandboxAccount();
    
    /**
     * 账户扣款
     * @param amount 扣款金额
     * @param passWord 支付密码
     * @return BaseResponse
     */
    @PostMapping("/deductBalance")
    BaseResponse deductBalance(@RequestParam("amount") Double amount, @RequestParam("passWord") String passWord);

    /**
     * 退款增加余额
     * @param amount 退款金额
     * @return BaseResponse
     */
    @PostMapping("/refundBalance")
    BaseResponse refundBalance(@RequestParam("amount") Double amount);

}
