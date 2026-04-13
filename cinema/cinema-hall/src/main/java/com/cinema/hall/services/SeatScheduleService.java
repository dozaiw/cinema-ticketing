package com.cinema.hall.services;

import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.SeatCacheData;
import com.cinema.hall.dto.PreselectSeatDto;
import com.cinema.hall.dto.ValidSeatDto;
import com.cinema.hall.entity.PreselectResult;

import java.util.List;

public interface SeatScheduleService {

    void initSeatSchedule(Integer scheduleId, Integer hallId);

    void deleteSeatSchedule(Integer scheduleId);

    Integer countSoldSeats(Integer scheduleId);

    PreselectResult preselectSeat(PreselectSeatDto preselectSeatDto) throws Exception;

    List<SeatCacheData> querySeatCondition(Integer scheduleId);

    BaseResponse cancelSeat(Integer scheduleId, Integer seatId) throws Exception;

    Boolean validSeat(ValidSeatDto validSeatDto);

    BaseResponse changeSeatStatus(List<Integer> seatIds,
                                  Integer status,
                                  Integer scheduleId,
                                  Integer orderId);
}
