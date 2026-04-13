package com.cinema.movie.services;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cinema.movie.entity.Movie;
import com.cinema.movie.mapper.MovieMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class MovieStatusScheduleService {

    @Autowired
    private MovieMapper movieMapper;

    /**
     * 应用启动时立即检查一次
     */
    @PostConstruct
    public void initCheck() {
        log.info("🎬 应用启动，开始检查电影下映状态...");
        checkMovieStatus();
    }

    /**
     * 每 24 小时检查一次
     * Cron 表达式：秒 分 时 日 月 周
     * 0 0 2 * * ? = 每天凌晨 2 点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledCheck() {
        log.info(" 定时任务触发，开始检查电影下映状态...");
        checkMovieStatus();
    }

    /**
     * 核心检查逻辑
     */
    private void checkMovieStatus() {
        try {
            Date now = new Date();
            
            // 1. 查询所有状态为 1（热映）且下映日期 <= 当前日期的电影
            List<Movie> expiredMovies = movieMapper.selectList(
                new LambdaQueryWrapper<Movie>()
                    .eq(Movie::getStatus, 1)  // 状态=热映
                    .le(Movie::getOfflineDate, now)  // 下映日期 <= 当前时间
                    .isNotNull(Movie::getOfflineDate)  // 下映日期不为空
            );

            if (expiredMovies.isEmpty()) {
                log.info("没有需要下映的电影");
                return;
            }

            log.info(" 发现 {} 部电影需要下映", expiredMovies.size());

            // 2. 批量更新状态为 0（已下映）
            for (Movie movie : expiredMovies) {
                updateMovieStatus(movie.getId(), 0);
                log.info(" 电影【{}】已自动下映（下映日期：{}）",
                    movie.getTitle(), 
                    movie.getOfflineDate());
            }

            log.info("电影下映状态检查完成，共更新 {} 部电影", expiredMovies.size());

        } catch (Exception e) {
            log.error(" 电影下映状态检查失败", e);
        }
    }

    /**
     * 更新电影状态
     */
    private void updateMovieStatus(Integer movieId, Integer status) {
        movieMapper.update(null, 
            new LambdaUpdateWrapper<Movie>()
                .eq(Movie::getId, movieId)
                .set(Movie::getStatus, status)
        );
    }

    /**
     * 手动触发检查
     */
    public void manualCheck() {
        log.info("🔧 手动触发电影下映状态检查...");
        checkMovieStatus();
    }
}