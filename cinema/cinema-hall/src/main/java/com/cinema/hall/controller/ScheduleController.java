package com.cinema.hall.controller;

import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.PageResult;
import com.cinema.common.entity.ResultCode;
import com.cinema.hall.dto.ScheduleQueryParams;
import com.cinema.hall.dto.SeatScheduleInitDto;
import com.cinema.hall.entity.Cinema;
import com.cinema.hall.entity.MovieScheduleCache;
import com.cinema.hall.entity.Schedule;
import com.cinema.hall.services.ScheduleService;
import com.cinema.hall.vo.ScheduleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * 排片表(Schedule)表控制层
 * @author makejava
 */
@Slf4j
@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    /**
     * 获取某电影某天的排片
     */
    @GetMapping("/public/{movieId}/schedules")
    public BaseResponse<Map<Long, Map<String, List<MovieScheduleCache.ScheduleInfo>>>> getSchedules(
            @PathVariable("movieId") Long movieId,
            @RequestParam(value = "date", required = false) String date) {

        try {
            LocalDate queryDate = (date == null || date.isEmpty())
                    ? LocalDate.now()
                    : LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            log.info("查询排片缓存：movieId={}, date={}", movieId, queryDate);

            Map<Long, Map<String, List<MovieScheduleCache.ScheduleInfo>>> data =
                    scheduleService.getMovieSchedules(movieId, queryDate);

            return BaseResponse.success(data != null ? data : Map.of());

        } catch (DateTimeParseException e) {
            log.warn("日期格式错误：date={}", date);
            return BaseResponse.error(403, "日期格式应为 yyyy-MM-dd");
        } catch (Exception e) {
            log.error("查询排片失败", e);
            return BaseResponse.error(ResultCode.SERVER_ERROR);
        }
    }

    /**
     * 获取某电影所有未来排片
     */
    @GetMapping("/public/{movieId}/schedules/all")
    public BaseResponse<Map<Long, Map<String, List<MovieScheduleCache.ScheduleInfo>>>> getAllFutureSchedules(
            @PathVariable("movieId") Long movieId) {

        try {
            log.info("查询所有未来排片：movieId={}", movieId);
            Map<Long, Map<String, List<MovieScheduleCache.ScheduleInfo>>> data =
                    scheduleService.getAllFutureSchedules(movieId);
            return BaseResponse.success(data);
        } catch (Exception e) {
            log.error("查询所有未来排片失败", e);
            return BaseResponse.error(ResultCode.SERVER_ERROR);
        }
    }

    /**
     * 获取某电影某天有排片的影院列表
     */
    @GetMapping("/public/{movieId}/date/{date}/cinemas")
    public BaseResponse<List<Cinema>> getCinemasByMovieAndDate(
            @PathVariable("movieId") Long movieId,
            @PathVariable("date") String date) {

        try {
            LocalDate queryDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            log.info("查询影院列表：movieId={}, date={}", movieId, queryDate);
            List<Cinema> data = scheduleService.getCinemasByMovieAndDate(movieId, queryDate);
            return BaseResponse.success(data);
        } catch (DateTimeParseException e) {
            return BaseResponse.error(403, "日期格式错误");
        } catch (Exception e) {
            log.error("查询影院列表失败", e);
            return BaseResponse.error(ResultCode.SERVER_ERROR);
        }
    }

    /**
     * 查询某电影在某影院某日的排片列表
     * 前端排片页面主接口
     */
    @GetMapping("/public/movie/{movieId}/cinema/{cinemaId}/date/{date}")
    public BaseResponse<List<ScheduleVO>> getSchedulesByMovieCinemaAndDate(
            @PathVariable("movieId") Long movieId,
            @PathVariable("cinemaId") Long cinemaId,
            @PathVariable("date") LocalDate date) {

        try {
            log.info("查询排片详情：movieId={}, cinemaId={}, date={}", movieId, cinemaId, date);

            // 参数校验
            if (movieId == null || cinemaId == null || date == null) {
                return BaseResponse.error(403, "缺少必要参数");
            }

            List<ScheduleVO> schedules = scheduleService.getSchedulesByMovieCinemaAndDate(movieId, cinemaId, date);

            log.info("查询成功，返回 {} 条排片", schedules.size());
            return BaseResponse.success(schedules);

        } catch (RuntimeException e) {
            // 业务异常（如影院不存在）
            log.warn("查询排片业务异常：{}", e.getMessage());
            return BaseResponse.error(403, e.getMessage());
        } catch (Exception e) {
            log.error("查询排片系统异常", e);
            return BaseResponse.error(403, "系统繁忙，请稍后重试");
        }
    }

    // ScheduleController.java - 修改 addSchedule 和 updateSchedule 方法

    /**
     * 添加排片
     */
    @PostMapping("/admin/add")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse addSchedule(@RequestBody Schedule schedule) {
        try {
            log.info("添加排片请求：{}", schedule);

            // 1. 基础参数校验
            if (schedule.getMovieId() == null || schedule.getHallId() == null || schedule.getStartTime() == null) {
                return BaseResponse.error(403, "缺少必要参数：movieId/hallId/startTime");
            }

            if (schedule.getEndTime() != null) {
                if (schedule.getEndTime().compareTo(schedule.getStartTime()) <= 0) {
                    return BaseResponse.error(403, "结束时间必须晚于开始时间");
                }
            }

            // 3. 检查时间段是否空闲
            if (!scheduleService.isTimeSlotFree(schedule)) {
                log.warn("时间段冲突：hallId={}, startTime={}, endTime={}",
                        schedule.getHallId(), schedule.getStartTime(), schedule.getEndTime());
                return BaseResponse.error(500, "该时间段已有排片");
            }

            scheduleService.addSchedule(schedule);
            log.info("添加排片成功：scheduleId={}", schedule.getId());
            return BaseResponse.success();

        } catch (RuntimeException e) {
            return BaseResponse.error(403, e.getMessage());
        } catch (Exception e) {
            log.error("添加排片失败", e);
            return BaseResponse.error(ResultCode.SERVER_ERROR);
        }
    }

    /**
     * 【管理】更新排片
     */
    @PostMapping("/admin/update")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse updateSchedule(@RequestBody Schedule schedule) {
        try {
            log.info("更新排片请求：{}", schedule);

            if (schedule.getId() == null) {
                return BaseResponse.error(403, "缺少排片 ID");
            }

            // 如果 endTime 不为 null，才做时间逻辑校验
            if (schedule.getEndTime() != null && schedule.getStartTime() != null) {
                if (schedule.getEndTime().compareTo(schedule.getStartTime()) <= 0) {
                    return BaseResponse.error(403, "结束时间必须晚于开始时间");
                }
            }

            // 检查时间段冲突
            if (!scheduleService.isTimeSlotFree(schedule)) {
                return BaseResponse.error(403, "该时间段与其他排片冲突");
            }

            scheduleService.updateSchedule(schedule);
            log.info("更新排片成功：scheduleId={}", schedule.getId());
            return BaseResponse.success();

        } catch (RuntimeException e) {
            return BaseResponse.error(403, e.getMessage());
        } catch (Exception e) {
            log.error("更新排片失败", e);
            return BaseResponse.error(ResultCode.SERVER_ERROR);
        }
    }

    /**
     * 删除排片
     */
    @PostMapping("/admin/delete/{scheduleId}")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse deleteSchedule(@PathVariable("scheduleId") Integer scheduleId) {
        try {
            log.info("删除排片请求：scheduleId={}", scheduleId);

            if (scheduleId == null || scheduleId <= 0) {
                return BaseResponse.error(403, "无效的排片ID");
            }

            scheduleService.deleteSchedule(scheduleId);
            log.info("删除排片成功：scheduleId={}", scheduleId);
            return BaseResponse.success();

        } catch (RuntimeException e) {
            return BaseResponse.error(403, e.getMessage());
        } catch (Exception e) {
            log.error("删除排片失败", e);
            return BaseResponse.error(ResultCode.SERVER_ERROR);
        }
    }

    /**
     * 获取所有排片
     */
    @GetMapping("/getAllSchedule")  //
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse<PageResult<ScheduleVO>> getAllSchedule(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

        try {
            log.info("查询所有排片：pageNum={}, pageSize={}", pageNum, pageSize);

            // 调用 Service（需要你先实现这个方法）
            PageResult<ScheduleVO> result = scheduleService.getAllSchedule(pageNum, pageSize);

            return BaseResponse.success(result);

        } catch (Exception e) {
            log.error("查询所有排片失败", e);
            return BaseResponse.error(ResultCode.SERVER_ERROR);
        }
    }

    /**
     * 获取今日排片数量统计
     */
    @GetMapping("/getTodaySchedule/{date}")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse getTodaySchedule(@PathVariable("date") String date) {
        try {
            // 校验日期格式
            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return scheduleService.getTodaySchedule(date);
        } catch (DateTimeParseException e) {
            return BaseResponse.error(403, "日期格式应为 yyyy-MM-dd");
        } catch (Exception e) {
            log.error("查询今日排片统计失败", e);
            return BaseResponse.error(ResultCode.SERVER_ERROR);
        }
    }


    @GetMapping("/public/query/movie/{movieId}/date/{date}")
    public BaseResponse queryMovieSchedules(
            @PathVariable("movieId") Integer movieId,
            @PathVariable("date") String date
    ) {
        try {
            // 参数校验
            if (movieId == null || movieId < 1) {
                return BaseResponse.error(403, "电影 ID 无效");
            }
            if (date == null || date.trim().isEmpty()) {
                return BaseResponse.error(403, "日期不能为空");
            }

            // 查询排片影院列表
            return scheduleService.queryMovieSchedules(movieId, date);
        } catch (Exception e) {
            log.error("查询电影排片失败", e);
            return BaseResponse.error(500, "查询失败：" + e.getMessage());
        }
    }

    //根据Id查询排片信息
    @GetMapping("/public/query/schedule/{scheduleId}")
    public BaseResponse queryScheduleById(@PathVariable("scheduleId") Integer scheduleId) {
        return scheduleService.queryScheduleById(scheduleId);
    }


    /**
     *  通用排片筛选接口（支持时间、影院、状态、电影等多条件组合）
     */
    @GetMapping("/admin/list/filtered")
    @PreAuthorize("hasAuthority('admin')")
  public BaseResponse<PageResult<ScheduleVO>> getFilteredScheduleList(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "movieId", required = false) Long movieId,
            @RequestParam(value = "cinemaId", required = false) Long cinemaId,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "status", required = false) Integer status) {
        
        try {
            log.info("筛选排片列表请求：pageNum={}, pageSize={}, movieId={}, cinemaId={}, date={}, status={}",
                    pageNum, pageSize, movieId, cinemaId, date, status);

            // 构建查询参数
            ScheduleQueryParams params = new ScheduleQueryParams();
            params.setPageNum(pageNum);
            params.setPageSize(pageSize);
            params.calculateOffset();

            // 设置筛选条件
        if (movieId != null) {
                params.setMovieId(movieId);
            }
        if (cinemaId != null) {
                params.setCinemaId(cinemaId);
            }

            // 日期参数（精确到天）
        if (date != null && !date.trim().isEmpty()) {
                params.setDate(LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }

            // 开始时间范围（精确到时分秒）
        if (startTime != null && !startTime.trim().isEmpty()) {
                params.setStartTime(LocalDateTime.parse(startTime,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }

            // 结束时间范围（精确到时分秒）
        if (endTime != null && !endTime.trim().isEmpty()) {
                params.setEndTime(LocalDateTime.parse(endTime,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }

            // 业务状态
        if (status != null) {
                params.setStatus(status);
            }

            // 调用 Service 查询
            PageResult<ScheduleVO> result = scheduleService.getFilteredScheduleList(params);

            log.info("筛选排片列表成功：总记录数={}, 当前页={}, 每页={}",
                    result.getTotal(), pageNum, pageSize);

            return BaseResponse.success(result);

        } catch (DateTimeParseException e) {
            log.warn("日期格式错误：date={}, startTime={}, endTime={}", date, startTime, endTime);
            return BaseResponse.error(403, "日期格式应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        } catch (Exception e) {
            log.error("筛选排片列表失败", e);
            return BaseResponse.error(ResultCode.SERVER_ERROR);
        }
    }

    @GetMapping("/getDetail/{scheduleId}")
    public ScheduleVO getCinemaNameAndHallNameByScheduleId(@PathVariable("scheduleId") Integer scheduleId) {
        return scheduleService.getCinemaNameAndHallNameByScheduleId(scheduleId);
    }

}