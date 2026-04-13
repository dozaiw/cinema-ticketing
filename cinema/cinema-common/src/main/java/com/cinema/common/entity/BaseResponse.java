package com.cinema.common.entity;

import java.io.Serializable;

/**
 * 全局统一响应类（通用所有接口，适配登录认证场景）
 * @param <T> 返回数据类型（如Token字符串、用户信息、空值等）
 */
public class BaseResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    // 响应状态码
    private int code;
    // 响应提示信息
    private String msg;
    // 响应数据
    private T data;

    // ========== 构造方法 ==========
    // 无参构造
    public BaseResponse() {}

    // 仅状态码+提示
    public BaseResponse(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    // 完整参数
    public BaseResponse(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 通用成功响应（无数据）
     */
    public static <T> BaseResponse<T> success() {
        return new BaseResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg());
    }

    /**
     * 成功响应（带数据，如登录成功返回Token）
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    /**
     * 成功响应（自定义提示+数据，如登录成功自定义提示）
     */
    public static <T> BaseResponse<T> success(String msg, T data) {
        return new BaseResponse<>(ResultCode.SUCCESS.getCode(), msg, data);
    }

    /**
     * 失败响应（直接传枚举，如用户名密码错误、Token无效）
     */
    public static <T> BaseResponse<T> error(ResultCode resultCode) {
        return new BaseResponse<>(resultCode.getCode(), resultCode.getMsg());
    }

    /**
     * 失败响应（自定义状态码+提示，适配特殊场景）
     */
    public static <T> BaseResponse<T> error(int code, String msg) {
        return new BaseResponse<>(code, msg);
    }

    // ========== getter/setter（外部序列化/解析需要） ==========
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}