//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.cinema.common.entity;

public enum ResultCode {
    SUCCESS(200, "操作成功"),
    LOGIN_SUCCESS(200, "登录成功"),
    PARAM_ERROR(400, "参数格式错误"),
    USERNAME_OR_PASSWORD_ERROR(401, "用户名或密码错误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    TOKEN_INVALID(401, "Token格式错误或签名无效"),
    FORBIDDEN(403, "无访问权限"),
    USER_NOT_EXIST(404, "用户不存在"),
    USER_LOCKED(403, "账号已被锁定"),
    USER_NOT_LOGIN(403, "用户未登录"),
    USER_NOT_LOGOUT(403, "用户未登出"),
    USER_REGISTER_FAILED(403, "用户注册失败"),
    USER_GET_ALL_GENRE_FAILED(403, "获取所有类型失败"),
    USER_ADD_GENRE_FAILED(403, "添加类型失败"),
    USER_UPDATE_GENRE_FAILED(403, "修改类型失败"),
    USER_DELETE_GENRE_FAILED(403, "删除类型失败"),
    USER_REGISTER_SUCCESS(200, "用户注册成功"),
    USER_EXIST(403, "用户已存在"),
    USER_DISABLED(401, "账号已被禁用，请联系管理员"),
    TOKEN_EXPIRED(401, "Token已过期"),
    USER_CHANGE_STATE_SUCCESS(200, "用户状态修改成功"),
    INTERNAL_SERVER_ERROR(500, "订单创建失败"),
    USER_CHANGE_STATE_FAILED(403, "用户状态修改失败"),
    SEAT_SCHEDULE_CANCEL_FAILED(403, "座位排期取消失败"),
    GET_HOT_MOVIE_LIST_ERROR(403, "获取热映影片列表失败"),
    USER_GET_ALL_MOVIE_FAILED(403, "获取所有影片列表失败"),
    SEAT_SCHEDULE_INIT_FAILED(403, "座位排期初始化失败"),
    PRESELECT_SEAT_FAILED(403, "座位预选失败"),
    TIME_SLOT_NOT_FREE(403, "时间段已占用"),
    SEAT_SCHEDULE_QUERY_FAILED(403, "座位排期查询失败"),
    INVALID_PARAMS(403, "参数格式错误"),
    SEAT_SCHEDULE_PRESELECT_FAILED(403, "座位排期预选失败"),
    SEAT_NOT_FOUND(404, "座位不存在"),
    SCHEDULE_NOT_FOUND(404, "排期不存在"),
    SCHEDULE_NOT_SELLING(404, "排期不存在或已下线"),
    SEAT_OCCUPIED(403, "座位已占用"),
    USER_CHANGE_MOVIE_FAILED(403, "修改影片失败"),
    SEAT_INIT_FAILED(403, "座位初始化失败"),
    SERVER_ERROR(500, "服务器内部错误"),
    DB_ERROR(500, "数据库操作异常"),
    USER_GET_MOVIE_BY_GENRE_FAILED(403, "获取影片列表失败"),
    USER_ADD_MOVIE_FAILED(403, "用户添加影片失败");

    private final int code;
    private final String msg;

    private ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return this.code;
    }

    public String getMsg() {
        return this.msg;
    }
}
