package com.cinema.hall.services;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.PageResult;
import com.cinema.hall.dto.ScheduleQueryParams;
import com.cinema.hall.entity.Cinema;
import com.cinema.hall.entity.MovieScheduleCache;
import com.cinema.hall.entity.Schedule;
import com.cinema.hall.vo.ScheduleVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 排片表(Schedule)表服务接口
 *
 * @author makejava
 * @since 2026-01-30 21:39:14
 */
public interface ScheduleService extends IService<Schedule> {

    /**
     * 获取某电影某天的排片
     * @return Map<cinemaId, Map<timeSlot, List<ScheduleInfo>>>
     */
    Map<Long, Map<String, List<MovieScheduleCache.ScheduleInfo>>> getMovieSchedules(
            Long movieId, LocalDate date);

    /**
     * 预热当天所有电影的排片缓存
     */
    void preloadTodaySchedules();

    /**
     * 获取某部电影的所有排片
     */
    Map<Long, Map<String, List<MovieScheduleCache.ScheduleInfo>>> getAllFutureSchedules(Long movieId);

    /**
     * 查询某电影某天的影院列表
     */
    List<Cinema> getCinemasByMovieAndDate(Long movieId, LocalDate date);

    /**
     * 添加排片
     */
    void addSchedule(Schedule schedule);

    /**
     * 更新排片
     */
    void updateSchedule(Schedule schedule);

    /**
     * 检查时间段是否空闲
     */
    boolean isTimeSlotFree(Schedule schedule);

    /**
     * 删除排片
     */
    void deleteSchedule(Integer scheduleId);

    List<ScheduleVO> getSchedulesByMovieCinemaAndDate(Long movieId, Long cinemaId, LocalDate date);

    PageResult<ScheduleVO> getAllSchedule(Integer pageNum, Integer pageSize);

    BaseResponse getTodaySchedule(String date);

    BaseResponse queryMovieSchedules(Integer movieId, String date);

    BaseResponse queryScheduleById(Integer scheduleId);

   PageResult<ScheduleVO> getFilteredScheduleList(ScheduleQueryParams params);

    ScheduleVO getCinemaNameAndHallNameByScheduleId(Integer scheduleId);
}