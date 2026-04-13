package com.cinema.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HallSalesStats {
   private String cinemaName;  // 影院名称
   private String hallName;    // 影厅名称
   private Long totalAmount;
   private Long orderCount;
   private Double percentage;
}
