package com.cinema.ticketing.services;// cinema-ticketing/src/main/java/com/cinema/ticketing/service/OrderService.java

import com.cinema.common.entity.BaseResponse;
import com.cinema.ticketing.entity.CreateOrderRequest;
import com.cinema.ticketing.entity.OrderResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

public interface OrderService {
    BaseResponse<OrderResponse> createOrder(CreateOrderRequest request);

    BaseResponse payOrder(Integer orderId,String passWord);

    @Transactional(rollbackFor = Exception.class)
    BaseResponse verifyTicket(String verifyCode);

    @Transactional
    BaseResponse cancelOrder(Integer orderId);

    @Transactional
    BaseResponse queryAllOrder();

    @Transactional
    BaseResponse queryTodayOrderCount(String date);

    @Transactional
    BaseResponse queryUserOrders(Integer userId);

    BaseResponse getOrderDetail(Integer orderId, Integer userId);

    BaseResponse adminQueryAllOrder();

    BaseResponse refundOrder(Integer orderId, Integer userId);

    /**
     * 获取当日各电影销售额度及占比
     */
    BaseResponse getMovieSalesStats(String dateStr);

    /**
     * 获取当日各电影院销售额度及占比
     */
    BaseResponse getHallSalesStats(String dateStr);

    /**
     * 获取当日订单状态分布
     */
    BaseResponse getOrderStatusStats(String dateStr);
}