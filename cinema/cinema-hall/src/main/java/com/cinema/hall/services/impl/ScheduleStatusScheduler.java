package com.cinema.hall.services.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cinema.hall.entity.Schedule;
import com.cinema.hall.mapper.ScheduleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 排片状态定时任务
 * <p>
 * 状态定义：
 * <ul>
 *     <li>0 - 未开始：当前时间 &lt; (startTime - 5分钟)</li>
 *     <li>1 - 进行中：(startTime - 5分钟) &lt;= 当前时间 &lt;= endTime</li>
 *     <li>2 - 已结束：当前时间 &gt; endTime</li>
 * </ul>
 * 执行频率：每分钟执行一次
 *
 * @author 吴梓烨
 * @date 2026-02-24
 */
@Slf4j
@Component
public class ScheduleStatusScheduler {

    @Autowired
    private ScheduleMapper scheduleMapper;

    /**
     * 每分钟执行一次，检查并更新排片状态
     */
    @Scheduled(initialDelay = 0, fixedRate = 60000)
    public void checkAndUpdateScheduleStatus() {
        try {
            Date now = new Date();
            // 关键边界：startTime <= now + 5min 等价于 (startTime - 5min) <= now
            Date fiveMinutesLater = new Date(now.getTime() + 5 * 60 * 1000);

            log.debug("开始检查排片状态: currentTime={}, boundary={}", now, fiveMinutesLater);

            // 【重要】执行顺序：先处理"已结束"→"进行中"→"未开始"，避免状态冲突
            updateToFinished(now);
            updateToOngoing(fiveMinutesLater, now);
            updateToNotStarted(fiveMinutesLater);

            log.debug("排片状态检查完成");

        } catch (Exception e) {
            log.error("排片状态检查失败", e);
        }
    }

    /**
     * 更新为"已结束"状态（2）
     * <p>
     * 条件：endTime < now 且 当前状态不是已结束
     *
     * @param now 当前时间
     */
    private void updateToFinished(Date now) {
        int count = scheduleMapper.update(null, new LambdaUpdateWrapper<Schedule>()
                .ne(Schedule::getStatus, Schedule.STATUS_FINISHED)
                .lt(Schedule::getEndTime, now)
                .set(Schedule::getStatus, Schedule.STATUS_FINISHED));

        if (count > 0) {
            log.info("批量更新排片为[已结束]: {} 条", count);
        }
    }

    /**
     * 更新为"进行中"状态（1）
     * <p>
     * 条件：startTime <= now+5min AND endTime >= now 且 当前状态不是进行中
     *
     * @param fiveMinutesLater now + 5分钟
     * @param now              当前时间
     */
    private void updateToOngoing(Date fiveMinutesLater, Date now) {
        int count = scheduleMapper.update(null, new LambdaUpdateWrapper<Schedule>()
                .ne(Schedule::getStatus, Schedule.STATUS_ONGOING)
                .le(Schedule::getStartTime, fiveMinutesLater)  // startTime <= now + 5min
                .ge(Schedule::getEndTime, now)                  // endTime >= now
                .set(Schedule::getStatus, Schedule.STATUS_ONGOING));

        if (count > 0) {
            log.info("批量更新排片为[进行中]: {} 条", count);
        }
    }

    /**
     * 更新为"未开始"状态（0）
     * <p>
     * 条件：startTime > now+5min 且 当前状态不是未开始
     *
     * @param fiveMinutesLater now + 5分钟
     */
    private void updateToNotStarted(Date fiveMinutesLater) {
        int count = scheduleMapper.update(null, new LambdaUpdateWrapper<Schedule>()
                .ne(Schedule::getStatus, Schedule.STATUS_NOT_STARTED)
                .gt(Schedule::getStartTime, fiveMinutesLater)  // startTime > now + 5min
                .set(Schedule::getStatus, Schedule.STATUS_NOT_STARTED));

        if (count > 0) {
            log.info("批量更新排片为[未开始]: {} 条", count);
        }
    }
}