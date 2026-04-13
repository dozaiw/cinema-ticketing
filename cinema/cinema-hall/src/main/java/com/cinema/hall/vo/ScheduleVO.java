// com.cinema.hall.vo.ScheduleVO.java
package com.cinema.hall.vo;

import com.cinema.hall.enums.ScheduleStatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 排片列表展示 VO
 * 状态动态计算：0-未开始，1-进行中，3-已结束
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleVO {

    /**
     * 排片ID
     */
    private Long id;

    /**
     * 电影ID
     */
    private Long movieId;

    /**
     * 影厅ID
     */
    private Long hallId;

    /**
     * 影厅名称
     */
    private String hallName;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    /**
     * 票价
     */
    private Double price;

    /**
     * 业务状态（1-正常，0-已删除）- 用于业务逻辑控制
     * @JsonIgnore 避免序列化时返回，前端只用 displayStatus
     */
    @JsonIgnore
    private Integer status;

    // ==================== 电影信息字段（用于前端展示）====================

    /**
     * 电影标题
     */
    private String movieTitle;

    /**
     * 电影海报URL
     */
    private String moviePoster;

    /**
     * 电影时长（分钟）
     */
    private Integer movieDuration;

    /**
     * 导演
     */
    private String movieDirector;

    /**
     * 主演
     */
    private String movieActors;

    // ==================== 影院信息字段 ====================

    /**
     * 影院ID
     */
    private Long cinemaId;

    /**
     * 影院名称
     */
    private String cinemaName;

    // ==================== 动态计算字段（只读，通过getter计算）====================

    /**
     * 🔥 动态计算的展示状态：0-未开始，1-进行中，3-已结束
     * Jackson 会自动调用 getter 序列化该字段
     */
    public Integer getDisplayStatus() {
        return ScheduleStatusEnum.calculateStatus(startTime, endTime);
    }

    /**
     * 状态描述文本
     */
    public String getDisplayStatusDesc() {
        return ScheduleStatusEnum.getByCode(getDisplayStatus()).getDesc();
    }

    /**
     * 状态标签类型（用于前端 el-tag type）
     */
    public String getDisplayStatusTagType() {
        return ScheduleStatusEnum.getByCode(getDisplayStatus()).getTagType();
    }

    /**
     * 获取开始时间（仅时间部分 HH:mm）
     */
    public String getStartTimeStr() {
        if (startTime == null) return "";
        return startTime.toLocalTime().toString().substring(0, 5);
    }

    /**
     * 获取结束时间（仅时间部分 HH:mm）
     */
    public String getEndTimeStr() {
        if (endTime == null) return "";
        return endTime.toLocalTime().toString().substring(0, 5);
    }

    /**
     * 是否可编辑：仅未开始且业务状态正常的排片可编辑
     */
    public boolean isEditable() {
        return getDisplayStatus().equals(0) && (status == null || status == 1);
    }

    /**
     * 是否可删除：仅未开始且业务状态正常的排片可删除
     */
    public boolean isDeletable() {
        return getDisplayStatus().equals(0) && (status == null || status == 1);
    }
}