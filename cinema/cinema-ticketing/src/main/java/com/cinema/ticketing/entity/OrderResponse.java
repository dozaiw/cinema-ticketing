package com.cinema.ticketing.entity;// cinema-common/src/main/java/com/cinema/common/dto/OrderResponse.java


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderNo;
    private Long scheduleId;
    private String hallName;
    private String movieName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime showTime;

    private List<Long> seatIds;
    private List<String> seatNames;
    private Long userId;
    private Integer amount;
    private String status;
    private String statusText;
    private String cinemaName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private String qrCodeUrl;
    private String qrCodeContent;
    private String verifyCode;
    private String userPhone;

    // ============ 微信支付参数（模拟用） ============
    private String prepayId;      // 预支付ID（模拟值）
    private String timeStamp;     // 时间戳
    private String nonceStr;      // 随机字符串
    private String packageValue;  // prepay_id=xxx
    private String signType;      // MD5
    private String paySign;       // 签名
}