package com.cinema.common.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类
 * 
 * @author 吴梓烨
 * @since 2026-01-30
 */
@Slf4j
public class DateUtils {
    
    /** 日期格式：yyyy-MM-dd */
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    
    /** 时间格式：HH:mm */
    public static final String TIME_PATTERN = "HH:mm";
    
    /** 日期时间格式：yyyy-MM-dd HH:mm:ss */
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    
    /** 日期格式器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    
    /** 时间格式器 */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);
    
    /** 日期时间格式器 */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
    
    /**
     * 将具体时间转换为时间段
     * 按3小时分组：09:00-12:00, 12:00-15:00, 15:00-18:00...
     * 
     * @param startTime 开始时间
     * @return 时间段字符串，格式：HH:mm-HH:mm
     */
    public static String getTimeSlot(LocalDateTime startTime) {
        if (startTime == null) {
            return "00:00-03:00"; // 默认值
        }
        
        int hour = startTime.getHour();
        int slotStart = (hour / 3) * 3;  // 按3小时分组
        int slotEnd = slotStart + 3;
        
        return String.format("%02d:00-%02d:00", slotStart, slotEnd);
    }
    
    /**
     * 将LocalDateTime格式化为日期字符串（yyyy-MM-dd）
     */
    public static String formatLocalDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }
    
    /**
     * 将LocalDateTime格式化为时间字符串（HH:mm）
     */
    public static String formatLocalTime(LocalTime time) {
        if (time == null) {
            return null;
        }
        return time.format(TIME_FORMATTER);
    }
    
    /**
     * 将LocalDateTime格式化为日期时间字符串（yyyy-MM-dd HH:mm:ss）
     */
    public static String formatLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATETIME_FORMATTER);
    }
    
    /**
     * 将日期字符串解析为LocalDate
     */
    public static LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
    
    /**
     * 将日期时间字符串解析为LocalDateTime
     */
    public static LocalDateTime parseLocalDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
    }
    
    /**
     * 获取今天的日期字符串（yyyy-MM-dd）
     */
    public static String getToday() {
        return LocalDate.now().format(DATE_FORMATTER);
    }
    
    /**
     * 判断两个日期是否是同一天
     */
    public static boolean isSameDay(LocalDateTime dt1, LocalDateTime dt2) {
        if (dt1 == null || dt2 == null) {
            return false;
        }
        return dt1.toLocalDate().equals(dt2.toLocalDate());
    }
    
    /**
     * 判断时间是否在5分钟内
     */
    public static boolean isWithin5Minutes(LocalDateTime targetTime) {
        if (targetTime == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !targetTime.isBefore(now.plusMinutes(5));
    }
    
    /**
     * 获取时间段的中文描述
     * 
     * @param timeSlot 格式：09:00-12:00
     * @return "上午", "下午", "晚上", "深夜"
     */
    public static String getTimeSlotDesc(String timeSlot) {
        if (timeSlot == null || timeSlot.length() < 5) {
            return "未知";
        }
        
        // 提取开始小时
        int startHour = Integer.parseInt(timeSlot.substring(0, 2));
        
        if (startHour >= 0 && startHour < 6) {
            return "深夜";
        } else if (startHour >= 6 && startHour < 12) {
            return "上午";
        } else if (startHour >= 12 && startHour < 18) {
            return "下午";
        } else {
            return "晚上";
        }
    }
    
    /**
     * 计算两个时间的时间差（分钟）
     */
    public static long getMinutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return java.time.Duration.between(start, end).toMinutes();
    }
}