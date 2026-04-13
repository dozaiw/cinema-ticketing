package com.cinema.ticketing.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cinema.common.entity.BaseResponse;
import com.cinema.ticketing.client.SeatScheduleFeignClient;
import com.cinema.ticketing.entity.Order;
import com.cinema.ticketing.mapper.OrderMapper;
import com.cinema.ticketing.services.SeatScheduleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.AccessType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单过期处理定时任务
 * 每分钟扫描一次过期订单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationTask {

    private final OrderMapper orderMapper;
    private final SeatScheduleFeignClient seatScheduleFeignClient;
    private final ObjectMapper objectMapper;
    @Autowired
    private SeatScheduleService seatScheduleService;

    /**
     * 每3秒
     */
    @Scheduled(fixedRate = 3000)
    public void checkExpiredOrders() {
        try {
            log.info("执行查看过期订单");
            // 1. 查询所有待支付且已过期的订单
            LocalDateTime now = LocalDateTime.now();
            List<Order> expiredOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                    .eq(Order::getStatus, "PENDING")
                    .lt(Order::getExpireTime, now)
            );

            if (expiredOrders.isEmpty()) {
                log.debug("无过期订单");
                return;
            }

            log.info("发现 {} 个过期订单，开始处理", expiredOrders.size());

            for (Order order : expiredOrders) {
                try {

                    // 3. 解析座位ID列表
                    List<Integer> seatIds = parseSeatIds(order.getSeatIds());
                    if (seatIds == null || seatIds.isEmpty()) {
                        log.warn("订单 {} 的座位ID为空", order.getOrderNo());
                        continue;
                    }

                    // 4. 释放座位（状态重置为0）
                    BaseResponse response = seatScheduleService.initSeatSchedule(
                            seatIds,
                            0,  // status=0 表示空闲
                            order.getScheduleId().intValue(),
                            0   // orderId=0 表示清除
                    );

                    if (response.getCode() == 200) {
                        log.info("订单 {} 的座位已释放", order.getOrderNo());
                    } else {
                        log.error("订单 {} 的座位释放失败: {}", order.getOrderNo(), response.error(403,"座位释放失败"));
                        throw new RuntimeException("座位释放失败");
                    }

                    // 2. 更新订单状态为已过期
                    order.setStatus("EXPIRED");
                    orderMapper.updateById(order);
                    log.info("订单 {} 已更新为过期状态", order.getOrderNo());

                } catch (Exception e) {
                    log.error("处理订单 {} 时出错", order.getOrderNo(), e);
                }
            }

        } catch (Exception e) {
            log.error("订单过期处理任务异常", e);
        }
    }

    /**
     * 解析座位ID JSON字符串
     */
    private List<Integer> parseSeatIds(String seatIdsJson) {
        if (seatIdsJson == null || seatIdsJson.isEmpty() || "[]".equals(seatIdsJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(seatIdsJson, List.class);
        } catch (JsonProcessingException e) {
            log.error("解析座位ID失败: {}", seatIdsJson, e);
            return null;
        }
    }
}