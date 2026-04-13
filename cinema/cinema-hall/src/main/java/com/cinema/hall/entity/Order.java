package com.cinema.hall.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 订单表(Order)实体类
 *
 * @author makejava
 * @since 2026-02-07 16:14:30
 */
public class Order implements Serializable {
    private static final long serialVersionUID = 229761372233348401L;
/**
     * 主键ID
     */
    private Long id;
/**
     * 用户ID
     */
    private Long userId;
/**
     * 场次ID
     */
    private Long scheduleId;
/**
     * 座位ID（JSON: [1,2,3]）
     */
    private String seatIds;
/**
     * 座位名称（JSON: ["A排5号","A排6号"]）
     */
    private String seatNames;
/**
     * 订单号（唯一）
     */
    private String orderNo;
/**
     * 订单金额（分）
     */
    private Integer totalAmount;
/**
     * 支付方式
     */
    private Integer payType;
/**
     * 支付时间
     */
    private Date payTime;
/**
     * 订单状态
     */
    private String status;
/**
     * 二维码图片URL
     */
    private String qrCodeUrl;
/**
     * 二维码内容
     */
    private String qrCodeContent;
/**
     * 验票码（6位数字）
     */
    private String verifyCode;
/**
     * 影厅名称
     */
    private String hallName;
/**
     * 电影名称
     */
    private String movieName;
/**
     * 场次时间
     */
    private Date showTime;
/**
     * 用户手机号（脱敏）
     */
    private String userPhone;
/**
     * 订单过期时间
     */
    private Date expireTime;
/**
     * 备注
     */
    private String remark;
/**
     * 创建时间
     */
    private Date createTime;
/**
     * 更新时间
     */
    private Date updateTime;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(String seatIds) {
        this.seatIds = seatIds;
    }

    public String getSeatNames() {
        return seatNames;
    }

    public void setSeatNames(String seatNames) {
        this.seatNames = seatNames;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getPayType() {
        return payType;
    }

    public void setPayType(Integer payType) {
        this.payType = payType;
    }

    public Date getPayTime() {
        return payTime;
    }

    public void setPayTime(Date payTime) {
        this.payTime = payTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getQrCodeContent() {
        return qrCodeContent;
    }

    public void setQrCodeContent(String qrCodeContent) {
        this.qrCodeContent = qrCodeContent;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public String getHallName() {
        return hallName;
    }

    public void setHallName(String hallName) {
        this.hallName = hallName;
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public Date getShowTime() {
        return showTime;
    }

    public void setShowTime(Date showTime) {
        this.showTime = showTime;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

}

