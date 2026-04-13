// cinema-common/src/main/java/com/cinema/common/dto/OrderResponse.java
package com.cinema.hall.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应
 * 
 * 定义位置：cinema-common（公共模块）
 * 使用方：cinema-ticketing → cinema-hall
 */
@Data
@Builder
public class OrderResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 订单ID（数据库主键）
     */
    private Long orderId;
    
    /**
     * 订单号（唯一）
     */
    private String orderNo;
    
    /**
     * 场次ID
     */
    private Long scheduleId;
    
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime showTime;
    
    /**
     * 座位ID列表
     */
    private List<Long> seatIds;
    
    /**
     * 座位名称列表
     */
    private List<String> seatNames;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 订单金额（分）
     */
    private Integer amount;
    
    /**
     * 订单状态
     */
    private String status;
    
    /**
     * 订单状态文本
     */
    private String statusText;
    
    /**
     * 过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 支付时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;
    
    /**
     * 二维码图片URL
     */
    private String qrCodeUrl;
    
    /**
     * 二维码内容字符串
     */
    private String qrCodeContent;
    
    /**
     * 验票码（6位数字）
     */
    private String verifyCode;
    
    /**
     * 用户手机号（脱敏）
     */
    private String userPhone;
}