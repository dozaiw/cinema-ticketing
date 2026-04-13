package com.cinema.hall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cinema.hall.entity.Seat;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SeatMapper extends BaseMapper<Seat> {

    @Select("SELECT * FROM seat WHERE hall_id = #{hallId}")
    List<Seat> selectByHallId(Integer hallId);
}
