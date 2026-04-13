package com.cinema.ticketing.client;

import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.ResultCode;
import com.cinema.ticketing.dto.PreselectResult;
import com.cinema.ticketing.dto.PreselectSeatDto;
import com.cinema.ticketing.entity.ValidSeatDto;
import com.cinema.ticketing.vo.ScheduleVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@FeignClient(value = "cinema-hall")
public interface SeatScheduleFeignClient {
    @PostMapping("/seatSchedule/public/valid")
    boolean valid(@RequestBody ValidSeatDto validSeatDto);

    @GetMapping("/seat/public/getSeatName")
    List<String> getSeatName(@RequestParam("seatIds") List<Integer> seatIds);

    @PostMapping("/seatSchedule/changSeatStatus")
    BaseResponse changeSeatStatus(@RequestParam("seatIds") List<Integer> seatIds,
                                  @RequestParam("status") Integer status,
                                  @RequestParam("schedule") Integer schedule,
                                  @RequestParam("orderId") Integer orderId);

    @PostMapping("/preselectSeat")
    BaseResponse<PreselectResult> preselectSeat(@RequestBody PreselectSeatDto dto);

    @GetMapping("/seatSchedule/public/query/seatCondition/{schedule}")
    BaseResponse querySeatCondition(@PathVariable("schedule") Integer schedule);

    @GetMapping("/schedule/getDetail/{scheduleId}")
    ScheduleVO getCinemaNameAndHallNameByScheduleId(@PathVariable("scheduleId") Integer scheduleId);
}