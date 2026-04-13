package com.cinema.hall.services.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cinema.auth.util.JwtUtil;
import com.cinema.auth.util.UserContextUtil;
import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.ResultCode;
import com.cinema.common.entity.SeatCacheData;
import com.cinema.hall.dto.PreselectSeatDto;
import com.cinema.hall.dto.ValidSeatDto;
import com.cinema.hall.entity.*;
import com.cinema.hall.mapper.ScheduleMapper;
import com.cinema.hall.mapper.SeatMapper;
import com.cinema.hall.mapper.SeatScheduleMapper;
import com.cinema.hall.services.SeatScheduleService;
import com.cinema.hall.vo.SeatConditionVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SeatScheduleServiceImpl extends ServiceImpl<SeatScheduleMapper, SeatSchedule>
        implements SeatScheduleService {

    @Autowired
    private SeatMapper seatMapper;

    @Autowired
    private SeatScheduleMapper seatScheduleMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;  // 用于座位缓存（JSON）

    @Autowired
    private StringRedisTemplate stringRedisTemplate;      // 用于锁座（纯字符串）

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserContextUtil userContextUtil;

    @Autowired
    private RedissonClient redissonClient;


    /**
     * 排片初始化：批量生成该场次的座位记录
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void initSeatSchedule(Integer scheduleId, Integer hallId) {
        log.info("初始化排片座位: scheduleId={}, hallId={}", scheduleId, hallId);

        LambdaQueryWrapper<Seat> seatQueryWrapper = new LambdaQueryWrapper<>();
        seatQueryWrapper.eq(Seat::getHallId, hallId);
        List<Seat> seatList = seatMapper.selectList(seatQueryWrapper);

        if (seatList == null || seatList.isEmpty()) {
            log.warn("影厅无座位: hallId={}", hallId);
            return;
        }

        List<SeatSchedule> seatScheduleList = seatList.stream()
                .map(seat -> {
                    SeatSchedule seatSchedule = new SeatSchedule();
                    seatSchedule.setScheduleId(scheduleId);
                    seatSchedule.setSeatStatus(seat.getStatus());
                    seatSchedule.setSeatId(seat.getId());
                    seatSchedule.setHallId(hallId);
                    return seatSchedule;
                })
                .collect(Collectors.toList());

        this.saveBatch(seatScheduleList);
        log.info("排片座位初始化成功: scheduleId={}, seatCount={}", scheduleId, seatScheduleList.size());
    }

    @Override
    @Transactional
    public void deleteSeatSchedule(Integer scheduleId) {
        int deleted = seatScheduleMapper.delete(
                new LambdaQueryWrapper<SeatSchedule>().eq(SeatSchedule::getScheduleId, scheduleId)
        );
        log.info("删除排片座位: scheduleId={}, deletedCount={}", scheduleId, deleted);
    }

    @Override
    public Integer countSoldSeats(Integer scheduleId) {
        return Math.toIntExact(seatScheduleMapper.selectCount(
                new LambdaQueryWrapper<SeatSchedule>()
                        .eq(SeatSchedule::getScheduleId, scheduleId)
                        .eq(SeatSchedule::getSeatStatus, 1)
        ));
    }

    @Override
    public PreselectResult preselectSeat(PreselectSeatDto dto) throws Exception {

        String lockKey = "lock:seat:preselect:" + dto.getScheduleId() + ":" + dto.getSeatId();

        RLock lock = redissonClient.getLock(lockKey);

        try {
            //尝试加锁
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);

            if (!locked) {
                throw new Exception("系统繁忙，请稍后重试");
            }


            Integer userId = userContextUtil.getUserId();

            if (userId == null) {
                throw new Exception("用户ID不能为空");
            }

            SeatSchedule seatSchedule = seatScheduleMapper
                    .selectOne(new LambdaQueryWrapper<SeatSchedule>()
                            .eq(SeatSchedule::getSeatId, dto.getSeatId())
                            .eq(SeatSchedule::getScheduleId, dto.getScheduleId()));

            if (seatSchedule == null) {
                throw new Exception("座位不存在");
            }

            Seat seat = seatMapper.selectById(seatSchedule.getSeatId());
            if (seat == null) {
                throw new Exception("座位信息异常");
            }

            Schedule schedule = scheduleMapper.selectById(seatSchedule.getScheduleId());
            if (schedule == null || schedule.getStartTime() == null) {
                throw new Exception("排片信息异常");
            }

            if (schedule.getStatus() != 0) {
                log.info("schedule={}", schedule.getStatus());
                throw new Exception("该场次已停止售票");
            }

            if (seatSchedule.getSeatStatus() == 1) {
                throw new Exception("座位已被占用");
            }

            // 计算过期时间
            long currentTime = System.currentTimeMillis();
            long startTime = schedule.getStartTime().getTime();
            long saleEndTime = startTime - 5 * 60 * 1000;
            long expireTimeMillis;
            long expireTimeSeconds;

            if (startTime - currentTime > 25 * 60 * 1000) {
                expireTimeMillis = currentTime + 20 * 60 * 1000;
                expireTimeSeconds = 20 * 60;
            } else {
                expireTimeMillis = saleEndTime;
                expireTimeSeconds = (expireTimeMillis - currentTime) / 1000;
                if (expireTimeSeconds < 60) {
                    expireTimeSeconds = 60;
                    expireTimeMillis = currentTime + 60 * 1000;
                }
            }

            // 先更新缓存，再锁座
            updateSeatCacheInRedis(schedule.getId(), seatSchedule.getId(), userId, expireTimeMillis);

            // 使用 StringRedisTemplate 锁座
            String preselectKey = "seat:preselect:" + schedule.getId() + ":" + seatSchedule.getSeatId();
            Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
                    preselectKey,
                    userId.toString(),
                    expireTimeSeconds,
                    TimeUnit.SECONDS
            );

            if (Boolean.FALSE.equals(success)) {
                // 锁座失败 → 回滚缓存
                rollbackSeatCacheUpdate(schedule.getId(), seatSchedule.getId());
                throw new Exception("座位已被占用");
            }

            // 构建返回结果
            PreselectResult result = new PreselectResult();
            result.setSeatRow(seat.getRowNum());
            result.setSeatCol(seat.getColNum());
            result.setExpireTime(expireTimeMillis);
            result.setRemainingSeconds(expireTimeSeconds);

            return result;

        } catch (InterruptedException e) {
            // ④ 单独处理中断异常
            Thread.currentThread().interrupt();
            log.error("获取锁被中断: userId={}, seatId={}",
                    userContextUtil.getUserId(), dto.getSeatId(), e);
            throw new Exception("请求被中断，请重试");

        } catch (Exception e) {
            Integer userId = userContextUtil.getUserId();
            log.error("预占座位失败: userId={}, seatId={}", userId, dto.getSeatId(), e);
            throw e;

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    /**
     * 回滚座位缓存更新
     */
    private void rollbackSeatCacheUpdate(Integer scheduleId, Integer seatScheduleId) {
        String redisKey = "seat:schedule:" + scheduleId;
        @SuppressWarnings("unchecked")
        List<SeatCacheData> seats = (List<SeatCacheData>) redisTemplate.opsForValue().get(redisKey);

        if (seats != null) {
            for (SeatCacheData seat : seats) {
                if (seat.getId().equals(seatScheduleId) && seat.getSeatStatus() == 1) {
                    seat.setSeatStatus(0);  // 恢复为空闲
                    seat.setUserId(null);
                    seat.setExpireTime(null);
                    break;
                }
            }
            redisTemplate.opsForValue().set(redisKey, seats,
                    redisTemplate.getExpire(redisKey, TimeUnit.SECONDS), TimeUnit.SECONDS);
            log.debug(" 回滚座位缓存: scheduleId={}, seatId={}", scheduleId, seatScheduleId);
        }
    }

    /**
     * 更新Redis座位缓存
     */
    private void updateSeatCacheInRedis(Integer scheduleId, Integer seatScheduleId,
                                        Integer userId, long expireTime) {
        String redisKey = "seat:schedule:" + scheduleId;

        @SuppressWarnings("unchecked")
        List<SeatCacheData> seats = (List<SeatCacheData>) redisTemplate.opsForValue().get(redisKey);

        if (seats == null || seats.isEmpty()) {
            log.debug("座位缓存不存在，跳过更新: scheduleId={}", scheduleId);
            return;
        }

        try {
            for (SeatCacheData seat : seats) {
                if (seat.getId().equals(seatScheduleId)) {
                    seat.setSeatStatus(1);
                    seat.setUserId(userId);
                    seat.setExpireTime(expireTime);
                    seat.setLastUpdateTime(System.currentTimeMillis());
                    break;
                }
            }

            Long originalTtl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            long newTtl = originalTtl != null && originalTtl > 0 ? originalTtl : 900;
            redisTemplate.opsForValue().set(redisKey, seats, newTtl, TimeUnit.SECONDS);
            log.debug("座位缓存更新成功: scheduleId={}, seatId={}", scheduleId, seatScheduleId);

        } catch (Exception e) {
            log.error("更新座位缓存失败，回滚锁座", e);
            String preselectKey = "seat:preselect:" + scheduleId + ":" + seatScheduleId;
            stringRedisTemplate.delete(preselectKey);  // 用 stringRedisTemplate 删除
            throw new RuntimeException("座位缓存更新失败", e);
        }
    }

    /**
     * 从Redis获取座位数据
     */
    private List<SeatCacheData> getSeatsFromRedis(String redisKey) {
        try {
            @SuppressWarnings("unchecked")
            List<SeatCacheData> seats = (List<SeatCacheData>) redisTemplate.opsForValue().get(redisKey);
            return seats;
        } catch (Exception e) {
            log.error("从Redis获取座位数据失败: redisKey={}", redisKey, e);
            return null;
        }
    }

    /**
     * 构建Redis Key
     */
    private String buildSeatCacheKey(Integer scheduleId) {
        return "seat:schedule:" + scheduleId;
    }

    /**
     * 从数据库初始化缓存（连表查询获取座位行列）
     */
    private List<SeatCacheData> initSeatCacheFromDatabase(Integer scheduleId) {
        try {
            Schedule schedule = scheduleMapper.selectById(scheduleId);
            if (schedule == null) {
                log.error("排片不存在: scheduleId={}", scheduleId);
                return Collections.emptyList();
            }

            if (schedule.getStartTime() == null) {
                log.error("排片开始时间为空: scheduleId={}", scheduleId);
                return Collections.emptyList();
            }

            Date saleEndTime = new Date(schedule.getStartTime().getTime() - 5 * 60 * 1000);
            Date now = new Date();

            if (now.after(saleEndTime)) {
                log.warn("排片已过售票结束时间: scheduleId={}, saleEndTime={}", scheduleId, saleEndTime);
                schedule.setStatus(3);
                scheduleMapper.updateById(schedule);
                return Collections.emptyList();
            }

            // 使用连表查询获取带 rowNum/colNum 的数据
            List<SeatConditionVO> dbSeats = seatScheduleMapper
                    .querySeatConditionWithRowCol(scheduleId);

            if (dbSeats == null || dbSeats.isEmpty()) {
                log.warn("排片无座位数据: scheduleId={}", scheduleId);
                return Collections.emptyList();
            }

            // 转换为 SeatCacheData（字段名一致可直接 copy）
            List<SeatCacheData> seats = dbSeats.stream().map(vo -> {
                SeatCacheData cache = new SeatCacheData();
                // 使用 BeanUtils 或手动 copy（推荐手动避免依赖）
                cache.setId(vo.getId());
                cache.setScheduleId(vo.getScheduleId());
                cache.setSeatId(vo.getSeatId());
                cache.setHallId(vo.getHallId());
                cache.setSeatStatus(vo.getSeatStatus());
                cache.setUserId(vo.getUserId());
                cache.setExpireTime(vo.getExpireTime());
                cache.setOrderId(Optional.ofNullable(vo.getOrderId())
                        .map(Integer::longValue)
                        .orElse(null));
                cache.setLastUpdateTime(vo.getLastUpdateTime() != null ?
                        vo.getLastUpdateTime().getTime() : null);
                cache.setRowNum(vo.getRowNum());
                cache.setColNum(vo.getColNum());
                return cache;
            }).collect(Collectors.toList());

            long currentTimeMillis = System.currentTimeMillis();
            long saleEndTimeMillis = saleEndTime.getTime();
            long cacheExpireSeconds = Math.max(15 * 60, (saleEndTimeMillis - currentTimeMillis) / 1000);

            if (cacheExpireSeconds <= 0) {
                cacheExpireSeconds = 60;
            }

            String redisKey = buildSeatCacheKey(scheduleId);
            redisTemplate.opsForValue().set(
                    redisKey,
                    seats,
                    cacheExpireSeconds,
                    TimeUnit.SECONDS
            );

            log.info("座位缓存初始化成功: scheduleId={}, seatCount={}, cacheExpire={}秒",
                    scheduleId, seats.size(), cacheExpireSeconds);

            scheduleMapper.updateById(schedule);

            return seats;

        } catch (Exception e) {
            log.error("初始化座位缓存失败: scheduleId={}", scheduleId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 查询座位
     */
    @Override
    public List<SeatCacheData> querySeatCondition(Integer scheduleId) {
        if (scheduleId == null || scheduleId <= 0) {
            log.error("排片ID无效: scheduleId={}", scheduleId);
            return Collections.emptyList();
        }

        String redisKey = buildSeatCacheKey(scheduleId);
        log.debug("查询座位缓存: scheduleId={}, redisKey={}", scheduleId, redisKey);

        List<SeatCacheData> seats = getSeatsFromRedis(redisKey);
        if (seats != null && !seats.isEmpty()) {
            log.debug("座位缓存命中: scheduleId={}, seatCount={}", scheduleId, seats.size());
            return seats;
        }

        log.info("座位缓存未命中，初始化缓存: scheduleId={}", scheduleId);
        seats = initSeatCacheFromDatabase(scheduleId);
        return seats != null ? seats : Collections.emptyList();
    }

    /**
     * 取消选座
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse cancelSeat(Integer scheduleId, Integer seatId) throws Exception {
        // ① 构造锁
        String lockKey = "lock:seat:cancel:" + scheduleId + ":" + seatId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // ② 尝试加锁
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new Exception("系统繁忙，请稍后重试");
            }

            // ========== 以下原有逻辑不变 ==========
            if (scheduleId == null || seatId == null) {
                throw new Exception("参数不能为空");
            }
            Integer userId = userContextUtil.getUserId();
            SeatSchedule seatSchedule = seatScheduleMapper.selectOne(new LambdaQueryWrapper<SeatSchedule>()
                    .eq(SeatSchedule::getScheduleId,scheduleId)
                    .eq(SeatSchedule::getSeatId,seatId));
            if (seatSchedule == null) {
                throw new Exception("座位不存在");
            }
            Integer seatScheduleId = seatSchedule.getId();

            String preselectKey = "seat:preselect:" + scheduleId + ":" + seatId;
            String lockedUserId = stringRedisTemplate.opsForValue().get(preselectKey);

            if (lockedUserId == null) {
                log.warn("座位未预占: scheduleId={}, seatId={}", scheduleId, seatId);
                throw new Exception("座位未预占或已被释放");
            }

            if (!lockedUserId.equals(userId.toString())) {
                log.warn("无权限取消选座: userId={}, lockedUserId={}", userId, lockedUserId);
                throw new Exception("无权限取消该座位");
            }

            stringRedisTemplate.delete(preselectKey);
            log.info("删除锁座记录: preselectKey={}", preselectKey);

            updateSeatCacheOnCancel(scheduleId, seatScheduleId, userId);

            log.info("取消选座成功: scheduleId={}, seatScheduleId={}, userId={}",
                    scheduleId, seatScheduleId, userId);

            return BaseResponse.success();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("取消选座被中断: userId={}, seatId={}",
                    userContextUtil.getUserId(), seatId, e);
            throw new Exception("请求被中断，请重试");

        } catch (Exception e) {
            log.error("取消选座失败", e);
            throw e;

        } finally {
            // ③ 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    // 判断生成订单座位的合法性
    @Override
    public Boolean validSeat(ValidSeatDto validSeatDto) {
        // 1. 入参判空
        Integer userId = validSeatDto.getUserId(); // 入参本身就是Integer
        List<Integer> seatIds = validSeatDto.getSeatIds();
        Integer scheduleId = validSeatDto.getScheduleId();
        List<Integer> SeatScheduleIds = new ArrayList<>();

        for (Integer seatId : seatIds) {
            SeatSchedule seatSchedule = seatScheduleMapper.selectOne(new LambdaQueryWrapper<SeatSchedule>().eq(SeatSchedule::getSeatId, seatId)
                    .eq(SeatSchedule::getScheduleId, scheduleId));
            SeatScheduleIds.add(seatSchedule.getId());
        }

        // 判空逻辑：校验Integer类型的userId，而非不存在的userIdStr
        if (ObjectUtils.isEmpty(userId) || CollectionUtils.isEmpty(seatIds) || scheduleId == null) {
            log.error("座位验证入参非法：userId={}, seatIds={}, scheduleId={}", userId, seatIds, scheduleId);
            return false;
        }

        // 2. Redis校验（核心逻辑正确，仅修正变量名）
        String keyPrefix = "seat:preselect:" + scheduleId + ":";
        for (Integer seatId : seatIds) {
            String key = keyPrefix + seatId;
            // 从Redis获取字符串类型的用户ID
            String preselectUserIdStr = stringRedisTemplate.opsForValue().get(key);

            // 先判空：Redis中无此座位的预占记录
            if (ObjectUtils.isEmpty(preselectUserIdStr)) {
                log.error("座位{}验证失败：Redis中无预占记录，Key={}", seatId, key);
                return false;
            }

            // 把Redis的字符串转成Integer，和入参的userId比较
            Integer preselectUserId;
            try {
                preselectUserId = Integer.parseInt(preselectUserIdStr);
            } catch (NumberFormatException e) {
                log.error("座位{}验证失败：Redis中预占用户ID格式错误，Key={}, 值={}", seatId, key, preselectUserIdStr);
                return false;
            }

            // 统一用Integer比较
            if (!ObjectUtils.nullSafeEquals(preselectUserId, userId)) {
                log.error("座位{}验证失败：Redis预占用户不匹配，预期={}，实际={}", seatId, userId, preselectUserId);
                return false;
            }
        }

        // 3. 批量查询数据库
        List<SeatSchedule> seatScheduleList = seatScheduleMapper.selectList(new LambdaQueryWrapper<SeatSchedule>()
                .in(SeatSchedule::getId, SeatScheduleIds));

        // 校验：部分座位无数据库记录
        if (seatScheduleList.size() != SeatScheduleIds.size()) {
            log.error("座位验证失败：部分座位无数据库记录，请求数={}，查询数={}", SeatScheduleIds.size(), seatScheduleList.size());
            return false;
        }

        // 遍历校验座位状态（0=未售出）
        for (SeatSchedule seatSchedule : seatScheduleList) {
            if (seatSchedule.getSeatStatus() != 0) {
                log.error("座位{}验证失败：已售出，状态={}", seatSchedule.getSeatId(), seatSchedule.getSeatStatus());
                return false;
            }
        }

        // 4. 所有校验通过，返回true
        return true;
    }

    @Override
    @Transactional
    public BaseResponse changeSeatStatus(List<Integer> seatIds, Integer status,
                                         Integer scheduleId, Integer orderId) {
        // ① 构造锁（注意：座位ID要排序，避免死锁）
        String sortedSeatIds = seatIds.stream()
                .map(String::valueOf)
                .sorted()
                .collect(Collectors.joining(","));
        String lockKey = "lock:seat:status:" + scheduleId + ":" + sortedSeatIds;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // ② 尝试加锁（等待时间稍长，因为涉及数据库操作）
            boolean locked = lock.tryLock(5, 15, TimeUnit.SECONDS);
            if (!locked) {
                return BaseResponse.error(503, "系统繁忙，请稍后重试");
            }

            // ========== 以下原有逻辑不变 ==========
            log.info("调用changeSeatStatus");
            Integer userId = userContextUtil.getUserId();

            List<SeatSchedule> seatSchedules = seatScheduleMapper.selectList(
                    new LambdaQueryWrapper<SeatSchedule>()
                            .eq(SeatSchedule::getScheduleId, scheduleId)
                            .in(SeatSchedule::getSeatId, seatIds)
            );

            for (SeatSchedule seatSchedule : seatSchedules){
                if (!Objects.equals(userId, seatSchedule.getUserId()) && seatSchedule.getUserId() != null){
                    return BaseResponse.error(403,"没有权限修改该座位信息");
                }
            }

            if (seatSchedules.size() != seatIds.size()) {
                return BaseResponse.error(404, "部分座位不存在");
            }

            for (SeatSchedule seatSchedule : seatSchedules) {
                if (seatSchedule.getSeatStatus() != 0) {
                    return BaseResponse.error(403, "座位已被占用，修改失败");
                }
            }

            seatScheduleMapper.update(null,
                    new LambdaUpdateWrapper<SeatSchedule>()
                            .set(SeatSchedule::getUserId, userId)
                            .set(SeatSchedule::getOrderId, orderId)
                            .set(SeatSchedule::getSeatStatus, 1)
                            .in(SeatSchedule::getSeatId, seatIds)
                            .eq(SeatSchedule::getScheduleId, scheduleId)
            );

            return BaseResponse.success("修改场次座位成功");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("修改座位状态被中断: scheduleId={}, seatIds={}", scheduleId, seatIds, e);
            return BaseResponse.error(500, "请求被中断");

        } catch (Exception e) {
            log.error("修改场次座位失败: {}", e.getMessage());
            return BaseResponse.error(500, "修改场次座位失败");

        } finally {
            // ③ 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    /**
     * 更新座位缓存状态
     */
    private void updateSeatCacheOnCancel(Integer scheduleId, Integer seatScheduleId, Integer userId) {
        String redisKey = "seat:schedule:" + scheduleId;
        @SuppressWarnings("unchecked")
        List<SeatCacheData> seats = (List<SeatCacheData>) redisTemplate.opsForValue().get(redisKey);

        if (seats == null || seats.isEmpty()) {
            log.debug("座位缓存不存在，跳过更新: scheduleId={}", scheduleId);
            return;
        }

        try {
            for (SeatCacheData seat : seats) {
                if (seat.getId().equals(seatScheduleId)) {
                    seat.setSeatStatus(0);
                    seat.setUserId(null);
                    seat.setExpireTime(null);
                    seat.setLastUpdateTime(System.currentTimeMillis());
                    break;
                }
            }

            Long originalTtl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            long newTtl = originalTtl != null && originalTtl > 0 ? originalTtl : 900;
            redisTemplate.opsForValue().set(redisKey, seats, newTtl, TimeUnit.SECONDS);
            log.debug("座位缓存更新成功（取消选座）: scheduleId={}, seatId={}", scheduleId, seatScheduleId);

        } catch (Exception e) {
            log.error("取消选座时更新缓存失败", e);
        }
    }

    private void updateSeatCacheExpireTime(Integer scheduleId, Integer seatScheduleId, long expireSeconds) {
        String redisKey = "seat:schedule:" + scheduleId;
        @SuppressWarnings("unchecked")
        List<SeatCacheData> seats = (List<SeatCacheData>) redisTemplate.opsForValue().get(redisKey);

        if (seats != null) {
            for (SeatCacheData seat : seats) {
                if (seat.getId().equals(seatScheduleId)) {
                    seat.setExpireTime(System.currentTimeMillis() + expireSeconds * 1000);
                    seat.setLastUpdateTime(System.currentTimeMillis());
                    break;
                }
            }
            redisTemplate.opsForValue().set(redisKey, seats, expireSeconds, TimeUnit.SECONDS);
        }
    }

    private void releaseSeatLockInRedis(Integer scheduleId, List<SeatSchedule> seatSchedules) {
        try {
            // 1. 删除 StringRedisTemplate 中的锁座记录（使用 seatId）
            for (SeatSchedule seatSchedule : seatSchedules) {
                String preselectKey = "seat:preselect:" + scheduleId + ":" + seatSchedule.getSeatId();
                Boolean deleted = stringRedisTemplate.delete(preselectKey);
                if (Boolean.TRUE.equals(deleted)) {
                    log.debug(" 删除锁座: {}", preselectKey);
                } else {
                    log.debug(" 锁座不存在或已过期: {}", preselectKey);
                }
            }

            // 2. 更新 Redis 缓存中的座位状态（使用 seatScheduleId）
            updateSeatCacheOnRelease(scheduleId, seatSchedules);

        } catch (Exception e) {
            log.error("释放 Redis 锁座失败: scheduleId={}", scheduleId, e);
            // 不抛出异常，避免影响数据库操作
        }
    }

    private void updateSeatCacheOnRelease(Integer scheduleId, List<SeatSchedule> seatSchedules) {
        String redisKey = "seat:schedule:" + scheduleId;

        @SuppressWarnings("unchecked")
        List<SeatCacheData> seats = (List<SeatCacheData>) redisTemplate.opsForValue().get(redisKey);

        if (seats == null || seats.isEmpty()) {
            log.debug("座位缓存不存在，跳过更新: scheduleId={}", scheduleId);
            return;
        }

        try {
            // 更新所有座位的状态为 0（空闲）
            for (SeatSchedule seatSchedule : seatSchedules) {
                for (SeatCacheData seat : seats) {
                    if (seat.getId().equals(seatSchedule.getId())) {
                        seat.setSeatStatus(0);      // 空闲
                        seat.setUserId(null);       // 清除用户
                        seat.setExpireTime(null);   // 清除过期时间
                        seat.setLastUpdateTime(System.currentTimeMillis());
                        log.debug("座位缓存已释放: scheduleId={}, seatId={}, seatScheduleId={}",
                                scheduleId, seatSchedule.getSeatId(), seatSchedule.getId());
                        break;
                    }
                }
            }

            // 保持原有 TTL
            Long originalTtl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            long newTtl = originalTtl != null && originalTtl > 0 ? originalTtl : 900;

            redisTemplate.opsForValue().set(redisKey, seats, newTtl, TimeUnit.SECONDS);
            log.info("座位缓存更新成功（释放座位）: scheduleId={}, 释放座位数={}",
                    scheduleId, seatSchedules.size());

        } catch (Exception e) {
            log.error(" 释放座位时更新缓存失败: scheduleId={}", scheduleId, e);
            // 不抛出异常，避免影响数据库操作
        }
    }

}