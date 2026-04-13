// com.cinema.hall.vo.SeatConditionVO.java
package com.cinema.hall.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.Date;

/**
 * 座位条件查询 VO（连表查询结果）
 * 包含 SeatSchedule + Seat 的字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatConditionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    // ========== SeatSchedule 字段 ==========
    private Integer id;                 // seat_schedule 主键
    private Integer scheduleId;         // 场次ID
    private Integer seatId;             // 座位ID
    private Integer hallId;             // 影厅ID
    private Integer seatStatus;         // 座位状态：0-可选，1-已售
    private Integer userId;             // 锁定用户ID（Redis）
    private Long expireTime;            // 锁定过期时间（Redis）
    private Integer orderId;            // 订单ID
    private Date lastUpdateTime;        // 最后更新时间

    // ========== Seat 表字段（连表查询得到） ==========
    private Integer rowNum;             // ✅ 行号
    private Integer colNum;             // ✅ 列号
}