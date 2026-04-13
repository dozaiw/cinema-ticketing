// cinema-ticketing/src/main/java/com/cinema/ticketing/controller/OrderController.java
package com.cinema.ticketing.controller;


import com.cinema.auth.util.JwtUtil;
import com.cinema.auth.util.UserContextUtil;
import com.cinema.common.entity.BaseResponse;

import com.cinema.ticketing.entity.CreateOrderRequest;
import com.cinema.ticketing.entity.OrderResponse;
import com.cinema.ticketing.services.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.xml.crypto.Data;
import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
   private OrderService orderService;
    @Autowired
   private JwtUtil jwtUtil;
    @Autowired
   private UserContextUtil userContextUtil;

    @PostMapping("/create")
    public BaseResponse<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest
    ) {
        // 1. 获取 Token 字符串
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
           token = token.substring(7);
        }
        if (token == null) {
          return BaseResponse.error(401, "Token 不存在");
        }

        // 2. 用 jwtUtil 直接解析 userId
        Integer userId = jwtUtil.getUserIdFromToken(token);
       request.setUserId(userId);

      return orderService.createOrder(request);
    }

    @PostMapping("/pay")
    public BaseResponse payOrder(
            @RequestParam("orderId") Integer orderId,
            @RequestParam("passWord") String passWord
    ) {
        try {
          return orderService.payOrder(orderId, passWord);
        } catch (RuntimeException e) {
           // 捕获运行时异常，返回 200 状态码和错误信息
           // 这样前端就能收到正常的错误提示而不是 403
           log.error("支付接口捕获异常：{}", e.getMessage());
         return BaseResponse.error(400, e.getMessage());
        } catch (Exception e) {
           log.error("支付接口未知异常：{}", e.getMessage());
         return BaseResponse.error(500, "支付处理失败：" + e.getMessage());
        }
    }

    @PostMapping("/cancel")
    public BaseResponse<OrderResponse> cancelOrder(
            @RequestParam("orderId") Integer orderId
    ) {
      return orderService.cancelOrder(orderId);
    }

    @GetMapping("/queryAllOrder")
    public BaseResponse<OrderResponse> queryAllOrder() {
      return orderService.queryAllOrder();
    }

    // 获取今日订单数量
    @GetMapping("/queryTodayOrderCount/{date}")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse queryTodayOrderCount(@PathVariable("date") String date) {
      return orderService.queryTodayOrderCount(date);
    }

    @GetMapping("/getOrderDetail/{orderId}")
    public BaseResponse<OrderResponse> getOrderDetail(@PathVariable("orderId") Integer orderId) {
        Integer userId = userContextUtil.getUserId();
      return orderService.getOrderDetail(orderId,userId);
    }

    @GetMapping("/adminQueryAllOrder")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse adminQueryAllOrder() {
      return orderService.adminQueryAllOrder();
    }

    @PostMapping("/refund")
    public BaseResponse refundOrder(@RequestParam("orderId") Integer orderId) {
        Integer userId = userContextUtil.getUserId();
      return orderService.refundOrder(orderId,userId);
    }

    @PostMapping("/verifyTicket")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse verifyTicket(@RequestParam("verifyCode") String verifyCode) {
      return orderService.verifyTicket(verifyCode);
    }

    /**
     * 获取当日订单各个电影销售额度及其占比
     * @param dateStr 日期字符串，格式：yyyy-MM-dd
     * @return 电影销售统计列表
     */
    @GetMapping("/getMovieSalesStats")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse getMovieSalesStats(@RequestParam("date") String dateStr) {
      return orderService.getMovieSalesStats(dateStr);
    }

    /**
     * 获取当日订单各个电影院的销售额度及其占比
     * @param dateStr 日期字符串，格式：yyyy-MM-dd
     * @return 电影院销售统计列表
     */
    @GetMapping("/getHallSalesStats")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse getHallSalesStats(@RequestParam("date") String dateStr) {
      return orderService.getHallSalesStats(dateStr);
    }

    /**
     * 获取当日订单状态分布
     * @param dateStr 日期字符串，格式：yyyy-MM-dd
     * @return 订单状态统计列表
     */
    @GetMapping("/getOrderStatusStats")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse getOrderStatusStats(@RequestParam("date") String dateStr) {
      return orderService.getOrderStatusStats(dateStr);
    }
}