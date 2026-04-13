package com.cinema.hall.services.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cinema.hall.entity.Cinema;
import com.cinema.hall.entity.Hall;
import com.cinema.hall.entity.Schedule;
import com.cinema.hall.entity.Seat;
import com.cinema.hall.mapper.HallMapper;
import com.cinema.hall.mapper.ScheduleMapper;
import com.cinema.hall.mapper.SeatMapper;
import com.cinema.hall.services.HallService;
import com.cinema.hall.services.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 影厅表(Hall)表服务实现类
 *
 * @author makejava
 * @since 2026-01-30 20:25:02
 */
@Service("hallService")
public class HallServiceImpl extends ServiceImpl<HallMapper, Hall> implements HallService {

    @Autowired
    private HallMapper hallMapper;
    @Autowired
    private SeatService seatService;
    @Autowired
    private SeatMapper seatMapper;
    @Autowired
    private ScheduleMapper scheduleMapper;

    @Override
    public List<Hall> getCinemaHalls(Integer cinemaId) {
        return hallMapper.selectList(new LambdaQueryWrapper<Hall>().eq(Hall::getCinemaId, cinemaId));
    }

    @Override
    public void addHall(Integer cinemaId, Hall hall) {
        hall.setCinemaId(cinemaId);
        hall.setStatus(1);
        hallMapper.insert(hall);
        seatService.initSeat(hall.getId());
    }

    @Override
    public void updateHall(Hall hall) {
        hallMapper.update(hall, new LambdaQueryWrapper<Hall>().eq(Hall::getId, hall.getId()));
    }

    @Override
    public void deleteHall(Integer hallId) {
        seatMapper.delete(new LambdaQueryWrapper<Seat>().eq(Seat::getHallId, hallId));
        scheduleMapper.delete(new LambdaQueryWrapper<Schedule>().eq(Schedule::getHallId, hallId));
        hallMapper.delete(new LambdaQueryWrapper<Hall>().eq(Hall::getId, hallId));
        /*
        退款
         */
    }

}
