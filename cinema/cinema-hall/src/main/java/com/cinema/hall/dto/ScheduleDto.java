package com.cinema.hall.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ScheduleDto {
    private Long scheduleId;
    private Long movieId;
    private Long cinemaId;
    private Long hallId;
    private String hallName; // 对应 h.name
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal price;
    private Integer status; // 对应 s.status
    private Integer totalSeats;
    private Integer soldSeats;
    private Integer remainSeats;
}