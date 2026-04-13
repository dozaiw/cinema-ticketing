// cinema-common/src/main/java/com/cinema/common/dto/CreateOrderRequest.java
package com.cinema.hall.entity;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 创建订单请求
 * 
 * 定义位置：cinema-common（公共模块）
 * 使用方：cinema-hall → cinema-ticketing
 */
@Data
@Builder
public class CreateOrderRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 场次ID
     */
    private Long scheduleId;
    
    /**
     * 座位ID列表
     */
    private List<Long> seatIds;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 影厅名称（冗余存储）
     */
    private String hallName;
    
    /**
     * 电影名称（冗余存储）
     */
    private String movieName;
    
    /**
     * 场次时间（冗余存储）
     */
    private String showTime;
    
    /**
     * 用户手机号（用于生成二维码）
     */
    private String userPhone;
}