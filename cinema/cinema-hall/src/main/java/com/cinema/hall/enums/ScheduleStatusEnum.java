package com.cinema.hall.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 排片展示状态枚举（动态计算）
 * 0-未开始，1-进行中，3-已结束
 */
@Getter
@AllArgsConstructor
public enum ScheduleStatusEnum {

    NOT_STARTED(0, "未开始", "info"),
    IN_PROGRESS(1, "进行中", "success"),
    ENDED(3, "已结束", "danger");

    private final Integer code;
    private final String desc;
    private final String tagType; // 对应 Element Plus 的 tag 类型

    /**
     * 根据时间动态计算状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 状态码 0/1/3
     */
    public static Integer calculateStatus(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return NOT_STARTED.getCode();
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            return NOT_STARTED.getCode();
        } else if (now.isAfter(endTime)) {
            return ENDED.getCode();
        } else {
            return IN_PROGRESS.getCode();
        }
    }

    /**
     * 根据状态码获取枚举
     */
    public static ScheduleStatusEnum getByCode(Integer code) {
        for (ScheduleStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return NOT_STARTED;
    }
}