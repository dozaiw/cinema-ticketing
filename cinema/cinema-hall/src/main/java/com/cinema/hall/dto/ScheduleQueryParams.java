// com.cinema.hall.dto.ScheduleQueryParams.java
package com.cinema.hall.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排片查询参数 DTO
 */
@Data
public class ScheduleQueryParams {

    /** 电影ID */
    private Long movieId;

    /** 影院ID */
    private Long cinemaId;

    /** 排片日期（精确到天） */
    private LocalDate date;

    /** 开始时间范围（>=） */
    private LocalDateTime startTime;

    /** 结束时间范围（<=） */
    private LocalDateTime endTime;

    /** 业务状态：1-正常，0-删除 */
  private Integer status;

    /** 分页参数 */
  private Integer pageNum = 1;
  private Integer pageSize= 10;

    /** MyBatis LIMIT 需要的 offset */
    private Integer offset;

    /** 排序字段（默认 start_time） */
    private String orderBy = "start_time";
    private String orderDir = "asc";

    /**根据 pageNum/pageSize 自动计算 offset */
    public void calculateOffset() {
        if (pageNum != null && pageSize != null && pageNum > 0 && pageSize > 0) {
            this.offset = (pageNum - 1) * pageSize;
        }
    }
}