package com.cinema.common.entity;

import lombok.Data;

/**
 * Redis座位数据结构
 */
@Data
public class SeatCacheData {
    private Integer seatId;
    private Integer seatStatus;        // 0=空闲, 1=预占, 2=锁定, 3=已售
    private Integer userId;         // 预占/锁定用户
    private Long expireTime;       // 过期时间戳（预占时使用）
    private Long orderId;          // 订单ID（锁定/已售后）
    private Long lastUpdateTime;   // 最后更新时间
    private Integer id;
    private Integer scheduleId;
    private Integer hallId;
    private Integer rowNum;
    private Integer colNum;
}