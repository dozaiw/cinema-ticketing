// cinema-common/src/main/java/com/cinema/common/mq/OrderPaidMessage.java
package com.cinema.ticketing.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class OrderPaidMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /** 订单ID */
    private Long orderId;
    
    /** 订单号 */
    private String orderNo;
    
    /** 场次ID */
    private Long scheduleId;
    
    /** 座位排片ID列表 */
    private List<Integer> seatScheduleIds;
}