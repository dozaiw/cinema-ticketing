package com.cinema.auth.services;

import com.cinema.auth.entity.User;
import com.cinema.common.entity.BaseResponse;

public interface UserService {
    BaseResponse regist(User var1);

    BaseResponse getUserInfo(String var1);

    BaseResponse getUserList(Integer pageNum, Integer pageSize);

    BaseResponse getFilteredUserList(Integer pageNum, Integer pageSize, String nickname, Integer status, Integer role);

    BaseResponse changeState(String var1, Integer var2);

    User findByPhone(String phone);
}
