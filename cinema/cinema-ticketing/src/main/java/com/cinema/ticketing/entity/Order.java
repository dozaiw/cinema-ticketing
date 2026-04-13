// cinema-ticketing/src/main/java/com/cinema/ticketing/entity/Order.java
package com.cinema.ticketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("cinema_order")
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long scheduleId;
    private String seatIds;       // JSON: [1,2,3]
    private String seatNames;     // JSON: ["A排5号","A排6号"]
    private String orderNo;
    private Integer totalAmount;
    private Integer payType;
    private LocalDateTime payTime;
    private String status;        // PENDING/PAID/EXPIRED/CANCELLED/USED
    private String qrCodeUrl;
    private String qrCodeContent;
    private String verifyCode;
    private String hallName;
    private String movieName;
    private LocalDateTime showTime;
    private String userPhone;
    private LocalDateTime expireTime;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}