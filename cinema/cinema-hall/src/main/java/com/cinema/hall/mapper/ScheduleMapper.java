// src/main/java/com/cinema/hall/mapper/ScheduleMapper.java
package com.cinema.hall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cinema.hall.dto.ScheduleDto;
import com.cinema.hall.dto.ScheduleQueryParams;
import com.cinema.hall.entity.Cinema;
import com.cinema.hall.entity.Schedule;
import com.cinema.hall.vo.ScheduleVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleMapper extends BaseMapper<Schedule> {
    
    /**
     * 查询某电影某天的有效排片（关联 hall + seat 表）
     * MyBatis-Plus 无法直接处理复杂 JOIN，需写 XML
     */
    List<ScheduleDto> selectByMovieAndDate(@Param("movieId") Long movieId, @Param("date") String date);
    
    /**
     * 查询所有电影某天的有效排片（用于缓存预热）
     */
    List<ScheduleDto> selectAllSchedulesByDate(@Param("date") String date);
    /**
     * 查询电影所有未来排片
     */
    List<ScheduleDto> selectAllFutureSchedules(@Param("movieId") Long movieId);

    // ScheduleMapper.java
    List<Cinema> selectCinemasByMovieAndDate(
            @Param("movieId") Long movieId,
            @Param("date") String date
    );

    List<ScheduleVO> selectSchedulesByMovieCinemaAndDate(
            @Param("movieId") Long movieId,
            @Param("cinemaId") Long cinemaId,
            @Param("date") LocalDate date
    );

    /**
     * 🔥 新增：通用条件查询排片列表（支持分页）
     */
    List<ScheduleVO> selectScheduleList(@Param("params") ScheduleQueryParams params);

    /**
     * 🔥 新增：统计符合条件的排片数量
     */
    Long countScheduleList(@Param("params") ScheduleQueryParams params);

}