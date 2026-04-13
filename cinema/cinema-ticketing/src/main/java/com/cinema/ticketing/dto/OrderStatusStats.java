package com.cinema.ticketing.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单状态统计 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusStats {
   private String status;
   private String statusText;
   private Long orderCount;
   private Double percentage;
}
