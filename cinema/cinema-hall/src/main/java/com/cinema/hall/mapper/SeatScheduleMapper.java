package com.cinema.hall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cinema.hall.entity.SeatSchedule;
import com.cinema.hall.vo.SeatConditionVO;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;

public interface SeatScheduleMapper extends BaseMapper<SeatSchedule> {
    List<SeatConditionVO> querySeatConditionWithRowCol(@Param("scheduleId") Integer scheduleId);
}
