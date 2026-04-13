package com.cinema.hall.services.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.PageResult;
import com.cinema.common.util.DateUtils;
import com.cinema.hall.dto.ScheduleDto;
import com.cinema.hall.dto.ScheduleQueryParams;
import com.cinema.hall.entity.*;
import com.cinema.hall.mapper.CinemaMapper;
import com.cinema.hall.mapper.HallMapper;
import com.cinema.hall.mapper.MovieMapper;
import com.cinema.hall.mapper.ScheduleMapper;
import com.cinema.hall.services.CinemaService;
import com.cinema.hall.services.HallService;
import com.cinema.hall.services.ScheduleService;
import com.cinema.hall.services.SeatScheduleService;
import com.cinema.hall.vo.ScheduleCinemaVO;
import com.cinema.hall.vo.ScheduleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements ScheduleService {

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private SeatScheduleService seatScheduleService;

    @Autowired
    private CinemaMapper cinemaMapper;

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private HallService hallService;

    @Autowired
    private CinemaService cinemaService;

    @Autowired
    private HallMapper hallMapper;


    private static final String REDIS_KEY_PREFIX = "movie:schedules:";
    private static final long CACHE_EXPIRE_SECONDS = 60; // 1 分钟

    // ==================== 缓存相关方法（保持不变）====================

    @Override
    public Map<Long, Map<String, List<MovieScheduleCache.ScheduleInfo>>> getMovieSchedules(
            Long movieId, LocalDate date) {

        String redisKey = buildRedisKey(date, movieId);

        String cachedJson = redisTemplate.opsForValue().get(redisKey);
        if (cachedJson != null && !cachedJson.isEmpty()) {
            log.debug("电影 {} 日期 {} 排片缓存命中", movieId, date);
            MovieScheduleCache fullCache = JSON.parseObject(cachedJson, MovieScheduleCache.class);
            return fullCache.getData().get(movieId);
        }

        log.warn("电影 {} 日期 {} 排片缓存未命中，查询数据库", movieId, date);
        List<ScheduleDto> schedules = scheduleMapper.selectByMovieAndDate(
                movieId,
                date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        MovieScheduleCache cache = new MovieScheduleCache();
        Map<Long, Map<Long, Map<String, List<MovieScheduleCache.ScheduleInfo>>>> data = new LinkedHashMap<>();
        data.put(movieId, buildCinemaMap(schedules));
        cache.setData(data);

        redisTemplate.opsForValue().set(
                redisKey,
                JSON.toJSONString(cache),
                CACHE_EXPIRE_SECONDS,
                TimeUnit.SECONDS
        );
        log.info("写入电影 {} 日期 {} 排片缓存，场次数量: {}", movieId, date, schedules.size());

        return cache.getData().get(movieId);
    }

    @Override
    public void preloadTodaySchedules() {
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        log.info("========== 开始预热 {} 的排片缓存 ==========", dateStr);

        List<ScheduleDto> allSchedules = scheduleMapper.selectAllSchedulesByDate(dateStr);

        if (allSchedules == null || allSchedules.isEmpty()) {
            log.warn("日期 {} 无有效排片", dateStr);
            return;
        }

        Map<Long, List<ScheduleDto>> movieGroups = new LinkedHashMap<>();
        for (ScheduleDto dto : allSchedules) {
            movieGroups.computeIfAbsent(dto.getMovieId(), k -> new ArrayList<>()).add(dto);
        }

        for (Map.Entry<Long, List<ScheduleDto>> entry : movieGroups.entrySet()) {
            Long movieId = entry.getKey();
            List<ScheduleDto> schedules = entry.getValue();

            MovieScheduleCache cache = new MovieScheduleCache();
            Map<Long, Map<Long, Map<String, List<MovieScheduleCache.ScheduleInfo>>>> data = new LinkedHashMap<>();
            data.put(movieId, buildCinemaMap(schedules));
            cache.setData(data);

            String redisKey = buildRedisKey(today, movieId);
            redisTemplate.opsForValue().set(
                    redisKey,
                    JSON.toJSONString(cache),
                    CACHE_EXPIRE_SECONDS,
                    TimeUnit.SECONDS
            );
        }

        log.info("========== 预热完成，共 {} 部电影 ==========", movieGroups.size());
    }

    @Override
    public Map<Long, Map<String, List<MovieScheduleCache.ScheduleInfo>>> getAllFutureSchedules(Long movieId) {
        String redisKey = "movie:schedules:future:" + movieId;

        String json = redisTemplate.opsForValue().get(redisKey);
        if (json != null && !json.isEmpty()) {
            MovieScheduleCache cache = JSON.parseObject(json, MovieScheduleCache.class);
            return cache.getData().get(movieId);
        }

        List<ScheduleDto> schedules = scheduleMapper.selectAllFutureSchedules(movieId);

        Map<Long, Map<String, List<MovieScheduleCache.ScheduleInfo>>> result = new LinkedHashMap<>();

        for (ScheduleDto dto : schedules) {
            Map<String, List<MovieScheduleCache.ScheduleInfo>> timeSlotMap =
                    result.computeIfAbsent(dto.getCinemaId(), k -> new LinkedHashMap<>());

            String timeSlot = DateUtils.getTimeSlot(dto.getStartTime());
            List<MovieScheduleCache.ScheduleInfo> list =
                    timeSlotMap.computeIfAbsent(timeSlot, k -> new ArrayList<>());

            MovieScheduleCache.ScheduleInfo info = new MovieScheduleCache.ScheduleInfo();
            info.setScheduleId(dto.getScheduleId());
            info.setHallId(dto.getHallId());
            info.setHallNumber(dto.getHallName());
            info.setStartTime(DateUtils.formatLocalTime(dto.getStartTime().toLocalTime()));
            info.setEndTime(DateUtils.formatLocalTime(dto.getEndTime().toLocalTime()));
            info.setPrice(dto.getPrice());
            info.setRemainSeats(dto.getRemainSeats() != null ? dto.getRemainSeats() : 100);
            list.add(info);
        }

        MovieScheduleCache cache = new MovieScheduleCache();
        Map<Long, Map<Long, Map<String, List<MovieScheduleCache.ScheduleInfo>>>> data = new LinkedHashMap<>();
        data.put(movieId, result);
        cache.setData(data);

        redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(cache), 3600, TimeUnit.SECONDS);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cinema> getCinemasByMovieAndDate(Long movieId, LocalDate date) {
        if (movieId == null || date == null) {
            log.warn("参数为空：movieId={}, date={}", movieId, date);
            return List.of();
        }

        log.info("查询电影在指定日期有排片的影院：movieId={}, date={}", movieId, date);

        List<Cinema> cinemas = scheduleMapper.selectCinemasByMovieAndDate(
                movieId,
                date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        List<Cinema> activeCinemas = cinemas.stream()
                .filter(cinema -> cinema.getStatus() != null && cinema.getStatus() == 1)
                .collect(Collectors.toList());

        log.info("查询到 {} 家营业中的影院（共{}家）", activeCinemas.size(), cinemas.size());
        return activeCinemas;
    }

    // ==================== addSchedule ====================

    @Override
    @Transactional
    public void addSchedule(Schedule schedule) {
        try {
            // 1. 参数校验
            if (schedule.getMovieId() == null || schedule.getHallId() == null || schedule.getStartTime() == null) {
                throw new RuntimeException("缺少必要参数：movieId/hallId/startTime");
            }

            // 2. 自动计算结束时间（如果前端没传）
            if (schedule.getEndTime() == null) {
                Movie movie = movieMapper.selectById(schedule.getMovieId());
                if (movie == null || movie.getDuration() == null || movie.getDuration() <= 0) {
                    throw new RuntimeException("电影时长未设置，无法计算结束时间");
                }

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(schedule.getStartTime());
                calendar.add(Calendar.MINUTE, movie.getDuration());  // + 电影时长
                calendar.add(Calendar.MINUTE, 15);                    // +15 分钟清场
                schedule.setEndTime(calendar.getTime());

                log.info("自动计算结束时间：movie={}, duration={}min, start={}, end={}",
                        movie.getTitle(), movie.getDuration(),
                        schedule.getStartTime(), schedule.getEndTime());
            }

            // 3. 校验时间逻辑（确保都不为 null 再比较）
            if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
                if (schedule.getEndTime().compareTo(schedule.getStartTime()) <= 0) {
                    throw new RuntimeException("结束时间必须晚于开始时间");
                }
            }

            // 4. 检查时间段是否空闲（isTimeSlotFree 内部会确保 endTime 不为 null）
            if (!isTimeSlotFree(schedule)) {
                throw new RuntimeException("该时间段已有排片");
            }

            // 5. 插入数据库
            schedule.setStatus(0);  // 默认正常
            scheduleMapper.insert(schedule);

            // 6. 初始化座位数据
            seatScheduleService.initSeatSchedule(schedule.getId(), schedule.getHallId());

            log.info("添加排片成功：scheduleId={}, movie={}, start={}, end={}",
                    schedule.getId(), schedule.getMovieId(),
                    schedule.getStartTime(), schedule.getEndTime());

        } catch (Exception e) {
            log.error("添加排片失败", e);
            throw new RuntimeException("添加排片失败：" + e.getMessage(), e);
        }
    }

    // ==================== 核心修复：updateSchedule ====================

    @Override
    @Transactional
    public void updateSchedule(Schedule schedule) {
        try {
            //  1. 查询原排片
            Schedule primarySchedule = scheduleMapper.selectById(schedule.getId());
            if (primarySchedule == null) {
                throw new RuntimeException("排片不存在");
            }

            //  2. 检查已售座位
            int soldSeats = seatScheduleService.countSoldSeats(schedule.getId());
            log.info("排片 {} 已售座位数: {}", schedule.getId(), soldSeats);

            //  3. 判断变更类型（用 Objects.equals 避免 NPE）
            boolean hallChanged = !Objects.equals(primarySchedule.getHallId(), schedule.getHallId());
            boolean movieChanged = !Objects.equals(primarySchedule.getMovieId(), schedule.getMovieId());
            boolean startTimeChanged = !Objects.equals(primarySchedule.getStartTime(), schedule.getStartTime());
            boolean priceChanged = !Objects.equals(primarySchedule.getPrice(), schedule.getPrice());

            log.info("排片 {} 变更检测 - 影厅:{}, 电影:{}, 开始时间:{}, 票价:{}",
                    schedule.getId(),
                    hallChanged ? "变更" : "不变",
                    movieChanged ? "变更" : "不变",
                    startTimeChanged ? "变更" : "不变",
                    priceChanged ? "变更" : "不变");

            //  4. 自动计算结束时间
            if (schedule.getEndTime() == null || movieChanged || startTimeChanged) {
                Movie movie = movieMapper.selectById(schedule.getMovieId());
                if (movie == null || movie.getDuration() == null || movie.getDuration() <= 0) {
                    throw new RuntimeException("电影时长未设置，无法计算结束时间");
                }
                if (schedule.getStartTime() == null) {
                    throw new RuntimeException("开始时间不能为空");
                }

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(schedule.getStartTime());
                calendar.add(Calendar.MINUTE, movie.getDuration());
                calendar.add(Calendar.MINUTE, 15);  // +15 分钟清场
                schedule.setEndTime(calendar.getTime());

                log.info("自动计算结束时间：movie={}, duration={}min, start={}, end={}",
                        movie.getTitle(), movie.getDuration(),
                        schedule.getStartTime(), schedule.getEndTime());
            }

            // 5. 校验时间逻辑
            if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
                if (schedule.getEndTime().compareTo(schedule.getStartTime()) <= 0) {
                    throw new RuntimeException("结束时间必须晚于开始时间");
                }
            }

            // 6. 业务逻辑处理
            if (soldSeats > 0) {
                if (hallChanged || movieChanged || startTimeChanged) {
                    log.warn("排片 {} 有 {} 张已售票，需要退款处理", schedule.getId(), soldSeats);
                    throw new RuntimeException(
                            String.format("该场次已有 %d 张已售出票，变更影厅/电影/时间需先退款处理", soldSeats)
                    );
                } else {
                    // 仅票价变更，直接更新
                    log.info("排片 {} 仅票价变更，直接更新", schedule.getId());
                    scheduleMapper.updateById(schedule);
                }
            } else {
                // 无已售座位，可安全操作
                if (hallChanged) {
                    log.info("排片 {} 影厅变更，重置座位数据", schedule.getId());
                    seatScheduleService.deleteSeatSchedule(schedule.getId());
                    seatScheduleService.initSeatSchedule(schedule.getId(), schedule.getHallId());
                }
                scheduleMapper.updateById(schedule);
                log.info("排片 {} 更新成功", schedule.getId());
            }

        } catch (Exception e) {
            log.error("排片更新业务异常: {}", e.getMessage());
            throw new RuntimeException("排片更新失败: " + e.getMessage(), e);
        }
    }

    // ==================== isTimeSlotFree ====================

    @Override
    public boolean isTimeSlotFree(Schedule schedule) {
        // 1. 判空保护
        if (schedule.getStartTime() == null) {
            log.warn("时间段校验失败：startTime 为空");
            return false;
        }

        // 2. 如果 endTime 为 null，尝试自动计算（用于时间冲突检测）
        if (schedule.getEndTime() == null && schedule.getMovieId() != null) {
            Movie movie = movieMapper.selectById(schedule.getMovieId());
            if (movie != null && movie.getDuration() != null && movie.getDuration() > 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(schedule.getStartTime());
                calendar.add(Calendar.MINUTE, movie.getDuration());
                calendar.add(Calendar.MINUTE, 15);  // +15 分钟清场
                schedule.setEndTime(calendar.getTime());
                log.debug("isTimeSlotFree 自动计算 endTime: {}", schedule.getEndTime());
            }
        }

        // 3. 如果还是 null，无法检测冲突，保守返回 false
        if (schedule.getEndTime() == null) {
            log.warn("时间段校验失败：endTime 仍为空，无法检测冲突");
            return false;
        }

        // 4. 时间冲突检测（此时 startTime/endTime 都不为 null）
        QueryWrapper<Schedule> wrapper = new QueryWrapper<>();
        wrapper.eq("hall_id", schedule.getHallId());

        // 排除自身（更新时）
        if (schedule.getId() != null) {
            wrapper.ne("id", schedule.getId());
        }

        // 两个时间段重叠的充要条件：A.start < B.end AND A.end > B.start
        wrapper.lt("start_time", schedule.getEndTime())
                .gt("end_time", schedule.getStartTime());

        boolean isFree = scheduleMapper.selectCount(wrapper) == 0;
        log.debug("时间段校验结果：hallId={}, start={}, end={}, isFree={}",
                schedule.getHallId(), schedule.getStartTime(), schedule.getEndTime(), isFree);
        return isFree;
    }

    // ==================== 其他方法（保持不变）====================

    @Override
    public void deleteSchedule(Integer scheduleId) {
        try {
            int soldSeats = seatScheduleService.countSoldSeats(scheduleId);
            if (soldSeats > 0) {
                log.warn("排片 {} 有 {} 张已售票，需要退款", scheduleId, soldSeats);
            }

            seatScheduleService.deleteSeatSchedule(scheduleId);
            scheduleMapper.delete(new LambdaQueryWrapper<Schedule>().eq(Schedule::getId, scheduleId));

            log.info("删除排片成功：ID={}", scheduleId);
        } catch (Exception e) {
            log.error("删除排片失败", e);
            throw new RuntimeException("删除排片失败", e);
        }
    }

    private Map<Long, Map<String, List<MovieScheduleCache.ScheduleInfo>>> buildCinemaMap(
            List<ScheduleDto> schedules) {

        Map<Long, Map<String, List<MovieScheduleCache.ScheduleInfo>>> cinemaMap = new LinkedHashMap<>();

        for (ScheduleDto dto : schedules) {
            Map<String, List<MovieScheduleCache.ScheduleInfo>> timeSlotMap =
                    cinemaMap.computeIfAbsent(dto.getCinemaId(), k -> new LinkedHashMap<>());

            String timeSlot = DateUtils.getTimeSlot(dto.getStartTime());
            List<MovieScheduleCache.ScheduleInfo> scheduleList =
                    timeSlotMap.computeIfAbsent(timeSlot, k -> new ArrayList<>());

            MovieScheduleCache.ScheduleInfo info = new MovieScheduleCache.ScheduleInfo();
            info.setScheduleId(dto.getScheduleId());
            info.setHallId(dto.getHallId());
            info.setHallNumber(dto.getHallName());
            info.setStartTime(DateUtils.formatLocalTime(dto.getStartTime().toLocalTime()));
            info.setEndTime(DateUtils.formatLocalTime(dto.getEndTime().toLocalTime()));
            info.setPrice(dto.getPrice());
            info.setRemainSeats(dto.getRemainSeats() != null ? dto.getRemainSeats() : 0);

            scheduleList.add(info);
        }

        return cinemaMap;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleVO> getSchedulesByMovieCinemaAndDate(Long movieId, Long cinemaId, LocalDate date) {
        try {
            log.info("查询排片：movieId={}, cinemaId={}, date={}", movieId, cinemaId, date);

            if (movieId == null || cinemaId == null || date == null) {
                log.warn("参数为空：movieId={}, cinemaId={}, date={}", movieId, cinemaId, date);
                return List.of();
            }

            Cinema cinema = cinemaMapper.selectById(cinemaId);
            if (cinema == null) {
                log.warn("影院不存在：cinemaId={}", cinemaId);
                throw new RuntimeException("影院不存在");
            }

            if (cinema.getStatus() == null || cinema.getStatus() == 0) {
                log.warn("影院已停运：cinemaId={}, name={}", cinemaId, cinema.getName());
                throw new RuntimeException("该影院已停运，无法查询排片");
            }

            List<ScheduleVO> schedules = scheduleMapper.selectSchedulesByMovieCinemaAndDate(movieId, cinemaId, date);
            //筛选出排片状态为0的
            schedules = schedules.stream()
                    .filter(schedule -> schedule.getStatus() == 0)
                    .collect(Collectors.toList());
            log.info("查询到 {} 条排片记录", schedules.size());
            return schedules;

        } catch (Exception e) {
            log.error("查询排片失败: movieId={}, cinemaId={}, date={}", movieId, cinemaId, date, e);
            throw new RuntimeException("查询排片失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ScheduleVO> getAllSchedule(Integer pageNum, Integer pageSize) {
        log.info("查询所有排片：pageNum={}, pageSize={}", pageNum, pageSize);

        int currentPage = (pageNum != null && pageNum > 0) ? pageNum : 1;
        int currentSize = (pageSize != null && pageSize > 0) ? pageSize : 20;
        int offset = (currentPage - 1) * currentSize;

        ScheduleQueryParams params = new ScheduleQueryParams();
        params.setOffset(offset);
        params.setPageSize(currentSize);

        List<ScheduleVO> records = scheduleMapper.selectScheduleList(params);
        Long total = scheduleMapper.countScheduleList(params);

        log.info("查询成功：total={}, records={}", total, records.size());

        return new PageResult<>(records, total, currentPage, currentSize);
    }

    @Override
    public BaseResponse getTodaySchedule(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);

            Date startDate = sdf.parse(date);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Date endDate = calendar.getTime();

            Long count = scheduleMapper.selectCount(new LambdaQueryWrapper<Schedule>()
                    .ge(Schedule::getStartTime, startDate)
                    .lt(Schedule::getEndTime, endDate));
            return BaseResponse.success(count);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public BaseResponse queryMovieSchedules(Integer movieId, String date) {
        try {
            // 1. 解析日期（格式：yyyy-MM-dd）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date targetDate = sdf.parse(date);

            // 2. 查询该电影该日期的所有排片（status=0 未开始）
            List<Schedule> schedules = this.list(
                    new LambdaQueryWrapper<Schedule>()
                            .eq(Schedule::getMovieId, movieId)
                            .eq(Schedule::getStatus, 0)
                            .orderByAsc(Schedule::getStartTime)
            );

            // 3. 过滤出指定日期的排片
            List<Schedule> filteredSchedules = schedules.stream()
                    .filter(schedule -> {
                        if (schedule.getStartTime() == null) return false;
                        String scheduleDate = sdf.format(schedule.getStartTime());
                        return scheduleDate.equals(date);
                    })
                    .collect(Collectors.toList());

            if (filteredSchedules.isEmpty()) {
                return BaseResponse.success(new ArrayList<>());
            }

            // 4. 预加载影厅和影院信息（避免N+1查询）
            Set<Integer> hallIds = filteredSchedules.stream()
                    .map(Schedule::getHallId)
                    .collect(Collectors.toSet());

            Map<Integer, Hall> hallMap = hallService.listByIds(hallIds)
                    .stream()
                    .collect(Collectors.toMap(Hall::getId, h -> h));

            Set<Integer> cinemaIds = hallMap.values().stream()
                    .map(Hall::getCinemaId)
                    .collect(Collectors.toSet());

            Map<Integer, Cinema> cinemaMap = cinemaService.listByIds(cinemaIds)
                    .stream()
                    .collect(Collectors.toMap(Cinema::getId, c -> c));

            // 5. 按影院ID分组，每组只保留价格最低的排片
            Map<Integer, Schedule> cinemaBestScheduleMap = new HashMap<>();

            for (Schedule schedule : filteredSchedules) {
                Hall hall = hallMap.get(schedule.getHallId());
                if (hall == null) continue;

                Integer cinemaId = hall.getCinemaId();
                Schedule currentBest = cinemaBestScheduleMap.get(cinemaId);

                // 如果当前影院还没有排片，或者当前排片价格更低，则更新
                if (currentBest == null || schedule.getPrice() < currentBest.getPrice()) {
                    cinemaBestScheduleMap.put(cinemaId, schedule);
                }
            }

            // 6. 组装 VO（每个影院只包含最低价的那一个排片）
            List<ScheduleCinemaVO> cinemaVOs = new ArrayList<>();

            for (Map.Entry<Integer, Schedule> entry : cinemaBestScheduleMap.entrySet()) {
                Integer cinemaId = entry.getKey();
                Schedule bestSchedule = entry.getValue();

                Cinema cinema = cinemaMap.get(cinemaId);
                if (cinema == null) continue;

                // 只包含最低价的那一个排片（单元素列表）
                List<Schedule> scheduleList = Collections.singletonList(bestSchedule);

                // minPrice 就是该排片的价格
                ScheduleCinemaVO vo = ScheduleCinemaVO.fromCinema(cinema, scheduleList, bestSchedule.getPrice());
                cinemaVOs.add(vo);
            }

            // 7. 按最低价格排序
            cinemaVOs.sort(Comparator.comparingDouble(ScheduleCinemaVO::getMinPrice));

            log.info("查询电影排片：movieId={}, date={}, 找到{}家影院（每家仅返回最低价排片）",
                    movieId, date, cinemaVOs.size());

            return BaseResponse.success(cinemaVOs);

        } catch (Exception e) {
            log.error("查询电影排片失败：movieId={}, date={}", movieId, date, e);
            return BaseResponse.error(500, "查询失败：" + e.getMessage());
        }
    }

    @Override
  public BaseResponse queryScheduleById(Integer scheduleId) {
        try {
            Schedule schedule = scheduleMapper.selectById(scheduleId);
            return BaseResponse.success(schedule);
        } catch (Exception e) {
            log.error("查询排片失败：scheduleId={}", scheduleId, e);
            return BaseResponse.error(500, "查询失败：" + e.getMessage());
        }
    }

    @Override
  public PageResult<ScheduleVO> getFilteredScheduleList(ScheduleQueryParams params) {
        try {
            log.info("筛选排片列表：params={}", params);

            // 1. 参数校验和默认值设置
        if (params.getPageNum() == null || params.getPageNum() < 1) {
                params.setPageNum(1);
            }
        if (params.getPageSize() == null || params.getPageSize() < 1) {
                params.setPageSize(20);
            }
            params.setPageSize(Math.min(params.getPageSize(), 50));
            params.calculateOffset();

            // 2. 查询数据库获取基础数据（带分页）
            List<ScheduleVO> records = scheduleMapper.selectScheduleList(params);
            Long total = scheduleMapper.countScheduleList(params);

            log.info("筛选排片列表成功：总记录数={}, 返回记录数={}", total, records.size());

            return new PageResult<>(records, total, params.getPageNum(), params.getPageSize());

        } catch (Exception e) {
            log.error("筛选排片列表失败", e);
            throw new RuntimeException("筛选排片列表失败：" + e.getMessage(), e);
        }
    }

    @Override
    public ScheduleVO getCinemaNameAndHallNameByScheduleId(Integer scheduleId) {
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        Integer hallId = schedule.getHallId();
        Hall hall = hallMapper.selectById(hallId);
        Integer cinemaId = hall.getCinemaId();
        Cinema cinema = cinemaMapper.selectById(cinemaId);
        String cinemaName = cinema.getName();
        String hallName = hall.getName();
        ScheduleVO scheduleVO = new ScheduleVO();
        scheduleVO.setCinemaName(cinemaName);
        scheduleVO.setHallName(hallName);
        return scheduleVO;
    }

    private String buildRedisKey(LocalDate date, Long movieId) {
        return REDIS_KEY_PREFIX +
                date.format(DateTimeFormatter.BASIC_ISO_DATE) +
                ":" + movieId;
    }

    // 简易 JSON 工具
    private static class JSON {
        public static <T> T parseObject(String json, Class<T> clazz) {
            return com.alibaba.fastjson.JSON.parseObject(json, clazz);
        }

        public static String toJSONString(Object obj) {
            return com.alibaba.fastjson.JSON.toJSONString(obj);
        }
    }
}