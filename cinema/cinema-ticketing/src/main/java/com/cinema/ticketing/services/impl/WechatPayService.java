package com.cinema.ticketing.services.impl;// cinema-ticketing/src/main/java/com/cinema/ticketing/service/WechatPayService.java


import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WechatPayService {

    @Data
    @Builder
    public static class WechatPayParams {
        private String prepayId;
        private String timeStamp;
        private String nonceStr;
        private String packageValue;
        private String signType;
        private String paySign;
    }

    /**
     * 模拟预支付
     */
    public WechatPayParams createPrepayOrder(String outTradeNo, Integer totalFee, String spbillCreateIp) {
        String nonceStr = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String prepayId = "wx_mock_" + System.currentTimeMillis();

        return WechatPayParams.builder()
                .prepayId(prepayId)
                .timeStamp(timeStamp)
                .nonceStr(nonceStr)
                .packageValue("prepay_id=" + prepayId)
                .signType("MD5")
                .paySign("mock_sign_" + outTradeNo)
                .build();
    }
}