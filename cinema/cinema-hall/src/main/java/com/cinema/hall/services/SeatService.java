package com.cinema.hall.services;

import com.cinema.hall.entity.Seat;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 座位表(Seat)表服务接口
 *
 * @author makejava
 * @since 2026-01-31 21:24:41
 */
public interface SeatService {
    void initSeat(Integer hallId);
    List<String> getSeatName(List<Integer> seatIds);
}
