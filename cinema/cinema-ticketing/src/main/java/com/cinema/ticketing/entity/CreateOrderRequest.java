package com.cinema.ticketing.entity;// cinema-common/src/main/java/com/cinema/common/dto/CreateOrderRequest.java


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer scheduleId;
    private List<Integer> seatIds;
    private Integer userId;
    private List<String> seatNames;
    private String hallName;
    private String cinemaName;
    private String movieName;
    private String showTime;      // 格式: "2026-02-10 19:00:00"
    private String userPhone;
    private Integer price;
}