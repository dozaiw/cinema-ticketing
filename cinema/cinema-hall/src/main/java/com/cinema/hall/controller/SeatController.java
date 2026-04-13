package com.cinema.hall.controller;

import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.ResultCode;
import com.cinema.hall.services.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 座位表(Seat)表控制层
 *
 * @author makejava
 * @since 2026-01-31 21:24:41
 */
@RestController
@RequestMapping("/seat")
public class SeatController {
    @Autowired
    private SeatService seatService;

    @PostMapping("/admin/init/{hallId}")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse initSeat(@PathVariable("hallId") Integer hallId) {
        try {
            seatService.initSeat(hallId);
            return BaseResponse.success();
        }catch (Exception e) {
            return BaseResponse.error(ResultCode.SEAT_INIT_FAILED);
        }
    }

    @GetMapping("/public/getSeatName")
    public List<String> getSeatName(@RequestParam("seatIds") List<Integer> seatIds) {
        return seatService.getSeatName(seatIds);
    }

}

