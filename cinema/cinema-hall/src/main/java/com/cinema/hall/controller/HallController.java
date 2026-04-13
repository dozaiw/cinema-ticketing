package com.cinema.hall.controller;

import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.ResultCode;
import com.cinema.hall.entity.Hall;
import com.cinema.hall.services.HallService;
import com.cinema.hall.services.SeatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/hall")
public class HallController {

    @Autowired
    private HallService hallService;
    @Autowired
    private SeatService seatService;

    @RequestMapping("/admin/{cinemaId}/halls")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse getCinemaHalls(@PathVariable("cinemaId") Integer cinemaId){
        try{
            return BaseResponse.success(hallService.getCinemaHalls(cinemaId));
        }catch (Exception e){
            log.error("查询影厅列表失败");
            return BaseResponse.error(ResultCode.SERVER_ERROR);
        }
    }

    @PostMapping("/admin/add/{cinemaId}")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse addHall(@PathVariable("cinemaId") Integer cinemaId,@RequestBody Hall hall){
        try{
            hall.setStatus(0);
            hallService.addHall(cinemaId, hall);
            return BaseResponse.success();
        }catch (Exception e){
            log.error("添加影厅失败");
            return BaseResponse.error(ResultCode.SERVER_ERROR);
        }
    }

    @PostMapping("/admin/update")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse updateHall(@RequestBody Hall hall){
        try{
            hallService.updateHall(hall);
            seatService.initSeat(hall.getId());
            return BaseResponse.success();
        }catch (Exception e){
            log.error("更新影厅失败");
            return BaseResponse.error(ResultCode.SERVER_ERROR);
        }
    }

    @PostMapping("/admin/delete/{hallId}")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse deleteHall(@PathVariable("hallId") Integer hallId){
        try{
            hallService.deleteHall(hallId);
            return BaseResponse.success();
        }catch (Exception e){
            log.error("删除影厅失败",e);
            return BaseResponse.error(ResultCode.SERVER_ERROR);
        }
    }

}
