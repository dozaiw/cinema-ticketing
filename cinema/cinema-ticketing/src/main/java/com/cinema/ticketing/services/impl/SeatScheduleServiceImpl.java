package com.cinema.ticketing.services.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.SeatCacheData;
import com.cinema.ticketing.entity.SeatSchedule;
import com.cinema.ticketing.mapper.SeatScheduleMapper;
import com.cinema.ticketing.services.SeatScheduleService;
import com.cinema.ticketing.util.OrderNoGenerator;
import com.cinema.ticketing.util.QrCodeGenerator;
import com.cinema.ticketing.util.QrCodeUtil;
import com.google.zxing.qrcode.encoder.QRCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class SeatScheduleServiceImpl extends ServiceImpl<SeatScheduleMapper, SeatSchedule>
        implements SeatScheduleService {


    @Autowired
    private SeatScheduleMapper seatScheduleMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;  // 用于座位缓存（JSON）

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public BaseResponse initSeatSchedule(List<Integer> seatIds, Integer status,
                                         Integer scheduleId, Integer orderId) {
        try {

            // 1. 查询座位
            List<SeatSchedule> seatSchedules = seatScheduleMapper.selectList(
                    new LambdaQueryWrapper<SeatSchedule>()
                            .eq(SeatSchedule::getScheduleId, scheduleId)
                            .in(SeatSchedule::getSeatId, seatIds)
            );

            // 3. 校验座位数量
            if (seatSchedules.size() != seatIds.size()) {
                return BaseResponse.error(404, "部分座位不存在");
            }

            // 4. 处理状态重置（status=0 表示释放座位）
            if (status == 0) {
                // 允许从任何状态重置（安全起见，只处理已锁定状态）
                for (SeatSchedule seatSchedule : seatSchedules) {
                    if (seatSchedule.getSeatStatus() != 1) {
                        log.warn("座位 {} 当前状态为 {}，无法释放",
                                seatSchedule.getId(), seatSchedule.getSeatStatus());
                        return BaseResponse.error(403, "座位状态异常，无法释放");
                    }
                }

                // 释放redis锁座
                releaseSeatLockInRedis(scheduleId, seatSchedules);

                // 释放座位：清除用户信息，重置状态
                seatScheduleMapper.update(null,
                        new LambdaUpdateWrapper<SeatSchedule>()
                                .set(SeatSchedule::getUserId, null)
                                .set(SeatSchedule::getOrderId, null)
                                .set(SeatSchedule::getSeatStatus, 0)  // 0 = 空闲
                                .in(SeatSchedule::getSeatId, seatIds)
                                .eq(SeatSchedule::getScheduleId, scheduleId)
                );
                return BaseResponse.success("座位已释放");
            }

            // 4. 判断座位状态
            for (SeatSchedule seatSchedule : seatSchedules) {
                if (seatSchedule.getSeatStatus() != 0) {
                    return BaseResponse.error(403, "座位已被占用，修改失败");
                }
            }

            return BaseResponse.success("修改场次座位成功");

        } catch (Exception e) {
            log.error("修改场次座位失败: {}", e.getMessage());
            return BaseResponse.error(500, "修改场次座位失败");
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
            log.error("释放座位时更新缓存失败: scheduleId={}", scheduleId, e);
            // 不抛出异常，避免影响数据库操作
        }
    }

}