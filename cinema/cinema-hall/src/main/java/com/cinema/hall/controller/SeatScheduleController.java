package com.cinema.hall.controller;

import com.cinema.auth.entity.User;
import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.ResultCode;
import com.cinema.hall.dto.PreselectSeatDto;
import com.cinema.hall.dto.SeatScheduleInitDto;
import com.cinema.hall.dto.ValidSeatDto;
import com.cinema.hall.entity.PreselectResult;
import com.cinema.hall.services.SeatScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seatSchedule")
@Slf4j
public class SeatScheduleController {

    @Autowired
    private SeatScheduleService seatScheduleService;

    @PostMapping("/init")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse initSeatSchedule(@RequestBody SeatScheduleInitDto seatScheduleInitDto) {
        try {
            Integer scheduleId = seatScheduleInitDto.getScheduleId();
            Integer hallId = seatScheduleInitDto.getHallId();
            seatScheduleService.initSeatSchedule(scheduleId, hallId);
            return BaseResponse.success();
        } catch (Exception e) {
            return BaseResponse.error(ResultCode.SEAT_SCHEDULE_INIT_FAILED);
        }
    }

    @GetMapping("/public/query/seatCondition/{schedule}")
    public BaseResponse querySeatCondition(@PathVariable("schedule") Integer schedule) {
        try {
            return BaseResponse.success(seatScheduleService.querySeatCondition(schedule));
        } catch (Exception e) {
            return BaseResponse.error(ResultCode.SEAT_SCHEDULE_QUERY_FAILED);
        }
    }

    @PostMapping("/preselectSeat")
    public BaseResponse<PreselectResult> preselectSeat(@RequestBody PreselectSeatDto dto) {
        try {
            //从 SecurityContext 获取 userId
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Object principal = auth.getPrincipal();

            // 检查是否为 User 类型
            if (!(principal instanceof User)) {
                throw new Exception("用户身份验证失败");
            }

            // 获取 userId
            Integer userId = ((User) principal).getId();  // 从 token 中获取的是字符串

            // 调用 Service
            PreselectResult result = seatScheduleService.preselectSeat(dto);

            // 返回包含数据的响应
            return BaseResponse.success(result);

        } catch (NumberFormatException e) {
            log.error("userId 格式错误", e);
            return BaseResponse.error(ResultCode.INVALID_PARAMS);
        } catch (Exception e) {
            log.error("预占座位失败", e);
            return BaseResponse.error(ResultCode.SEAT_SCHEDULE_PRESELECT_FAILED);
        }
    }

    @PostMapping("/cancelSeat/{scheduleId}/{seatId}")
    public BaseResponse cancelSeat(@PathVariable("scheduleId") Integer scheduleId,@PathVariable("seatId") Integer seatId) {
        try {
            // 从 SecurityContext 获取 userId（转换为 Integer）
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Object principal = auth.getPrincipal();

            //  检查是否为 User 类型
            if (!(principal instanceof User)) {
                throw new Exception("用户身份验证失败");
            }

            // 获取 userId
            Integer userId = ((User) principal).getId();  // 从 token 中获取的是字符串

            seatScheduleService.cancelSeat(scheduleId, seatId);
        }catch (Exception e){
            return BaseResponse.error(ResultCode.SEAT_SCHEDULE_CANCEL_FAILED);
        }
        return BaseResponse.success();
    }

    @PostMapping("/public/valid")
    public Boolean valid(@RequestBody ValidSeatDto validSeatDto) {
        return seatScheduleService.validSeat(validSeatDto);
    }

    @PostMapping("/changSeatStatus")
    BaseResponse changeSeatStatus(@RequestParam("seatIds") List<Integer> seatIds,
                                  @RequestParam("status") Integer status,
                                  @RequestParam("schedule") Integer schedule,
                                  @RequestParam("orderId") Integer orderId){
        return seatScheduleService.changeSeatStatus(seatIds, status, schedule,orderId);
    }

}

