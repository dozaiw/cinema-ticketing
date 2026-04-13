package com.cinema.ticketing.dto;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 电影销售统计 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieSalesStats {
   private String movieName;
   private Long totalAmount;
   private Long orderCount;
   private Double percentage;
}
