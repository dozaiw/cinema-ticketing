package com.cinema.ticketing.services;

import com.cinema.common.entity.BaseResponse;

import java.util.List;

public interface SeatScheduleService {
    BaseResponse initSeatSchedule(List<Integer> seatIds,Integer status,Integer schedule,Integer orderId);
}
