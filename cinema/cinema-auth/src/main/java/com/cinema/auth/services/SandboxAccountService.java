package com.cinema.auth.services;

import com.cinema.common.entity.BaseResponse;

public interface SandboxAccountService {

     boolean createAccount(Integer userId);

     BaseResponse deductBalance(Integer userId, Double amount,String passWord);

     Double getBalance(Integer userId);

     BaseResponse refundBalance(Integer userId, Double amount);
}
