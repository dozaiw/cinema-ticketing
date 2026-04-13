package com.cinema.hall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.util.Date;
import java.io.Serializable;

/**
 * 场次-座位关联表（排片初始化时创建，支付后补充订单信息）(SeatSchedule)实体类
 *
 * @author makejava
 * @since 2026-02-03 16:41:00
 */
public class SeatSchedule implements Serializable {
    private static final long serialVersionUID = 190155527139489389L;
/**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;
/**
     * 场次ID（关联schedule表主键，排片时必填）
     */
    private Integer scheduleId;
/**
     * 座位ID（关联seat表主键，排片时必填）
     */
    private Integer seatId;
/**
     * 影厅ID（关联hall表主键，排片时必填，冗余优化查询）
     */
    private Integer hallId;
/**
     * 订单ID（关联order表主键，支付成功后填充，允许为空）
     */
    private Integer orderId;
/**
     * 购票用户ID（关联user表主键，支付成功后填充，允许为空）
     */
    private Integer userId;
/**
     * 座位最终状态：0-可选，2-已售出（临时锁定由Redis管理）
     */
    private Integer seatStatus;
/**
     * 记录创建时间（排片初始化时自动生成）
     */
    private Date createTime;
/**
     * 状态更新时间（支付/退票时自动更新）
     */
    private Date updateTime;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Integer scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Integer getSeatId() {
        return seatId;
    }

    public void setSeatId(Integer seatId) {
        this.seatId = seatId;
    }

    public Integer getHallId() {
        return hallId;
    }

    public void setHallId(Integer hallId) {
        this.hallId = hallId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getSeatStatus() {
        return seatStatus;
    }

    public void setSeatStatus(Integer seatStatus) {
        this.seatStatus = seatStatus;
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

