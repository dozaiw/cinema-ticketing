package com.cinema.ticketing.dto;

import lombok.Data;

@Data
public class PreselectResult {
    private Integer seatRow;          // 行号
    private Integer seatCol;          // 列号
    private Long expireTime;          // 过期时间戳
    private Long remainingSeconds;    // 剩余秒数
    
    // 保留其他字段...
}