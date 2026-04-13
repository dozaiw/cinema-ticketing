package com.cinema.auth.controller;

import com.cinema.auth.services.UserService;
import com.cinema.auth.util.UserContextUtil;
import com.cinema.common.entity.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/auth/user"})
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private UserContextUtil userContextUtil;

    @GetMapping({"/info"})
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse getUserInfo(@RequestParam("username") String username) {
        return this.userService.getUserInfo(username);
    }


    @GetMapping("/current")
    public BaseResponse getCurrentUserInfo() {
        String username = userContextUtil.getUsername();
        return this.userService.getUserInfo(username);
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('admin')")
   public BaseResponse getUserList(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return userService.getUserList(pageNum, pageSize);
    }

    @GetMapping("/list/filtered")
    @PreAuthorize("hasAuthority('admin')")
   public BaseResponse getFilteredUserList(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "role", required = false) Integer role) {
        return userService.getFilteredUserList(pageNum, pageSize, nickname, status, role);
    }

    @PostMapping({"/admin/changeState"})
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse changeState(@RequestParam("username") String username, @RequestParam("state") Integer state) {
        return this.userService.changeState(username, state);
    }
}
