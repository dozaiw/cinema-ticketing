package com.cinema.hall.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 电影排片缓存结构
 * 结构：Map<movieId, Map<cinemaId, Map<timeSlot, List<ScheduleInfo>>>>
 */
@Data
public class MovieScheduleCache {
    
    /**
     * 核心数据结构
     * Key1: movieId (Long)
     *   → Key2: cinemaId (Long)
     *     → Key3: timeSlot (String, 格式 "09:00-12:00")
     *       → Value: List<ScheduleInfo>
     */
    private Map<Long, Map<Long, Map<String, List<ScheduleInfo>>>> data;
    
    @Data
    public static class ScheduleInfo {
        private Long scheduleId;    // 排片ID
        private Long hallId;        // 影厅ID
        private String hallNumber;  // 影厅号
        private String startTime;   // HH:mm
        private String endTime;     // HH:mm
        private BigDecimal price;   // 票价
        private Integer remainSeats; // 剩余座位数
    }
}