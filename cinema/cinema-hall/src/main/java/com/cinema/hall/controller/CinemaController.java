package com.cinema.hall.controller;


import com.cinema.common.entity.BaseResponse;
import com.cinema.hall.dto.CinemaDTO;
import com.cinema.hall.entity.Cinema;
import com.cinema.hall.services.CinemaService;
import com.cinema.hall.util.LocationUtil;
import com.cinema.hall.vo.NearbyCinemaVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 影院管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/cinema")
public class CinemaController {

    @Autowired
    private CinemaService cinemaService;

    /**
     * 添加影院
     */
    @PostMapping("/admin/add")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse addCinema(@RequestBody CinemaDTO cinemaDTO) {
        return cinemaService.add(cinemaDTO);
    }

    /**
     * 修改影院信息
     */
    @PostMapping("/admin/update")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse updateCinema(@RequestBody CinemaDTO cinemaDTO) {
        return cinemaService.update(cinemaDTO);
    }

    /**
     * 修改影院状态
     */
    @PostMapping("/admin/changeStatus")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse changeStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        return cinemaService.changeStatus(id, status);
    }

    /**
     * 删除影院
     */
    @DeleteMapping("/admin/delete/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse deleteCinema(@PathVariable("id") Long id) {
        return cinemaService.delete(id);
    }

    /**
     * 查询影院列表（分页）
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse listCinemas(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        return cinemaService.listPage(pageNum, pageSize, name, city, status);
    }

    /**
     * 查询影院详情
     */
    @GetMapping("/public/detail/{id}")
    public BaseResponse getCinemaDetail(@PathVariable("id") Long id) {
        return cinemaService.getById(id);
    }


    /**
     * 按城市查询影院
     */
    @GetMapping("/public/byCity/{city}")
    public BaseResponse getCinemasByCity(@PathVariable("city") String city) {
        return cinemaService.findByCity(city);
    }

    /**
     * 地址转经纬度（地理编码）
     */
    @PostMapping("/admin/geocode")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse geocodeAddress(@RequestParam("address") String address) {
        return cinemaService.geocodeAddress(address);
    }


    // 获取营业影厅数量
    @GetMapping("/getWorkingCinemaCount")
    public BaseResponse getWorkingCinemaCount() {
        return cinemaService.getWorkingCinemaCount();
    }

    @GetMapping("/nearby")
    public BaseResponse getNearbyCinemas(
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude
    ) {
        try {
            // 参数校验
            if (!LocationUtil.isValidCoordinate(latitude, longitude)) {
                return BaseResponse.error(403, "经纬度格式错误");
            }

            List<NearbyCinemaVO> cinemaList = cinemaService.getNearbyCinemas(latitude, longitude);
            return BaseResponse.success(cinemaList);
        } catch (Exception e) {
            log.error("查询附近影院失败", e);
            return BaseResponse.error(500, "查询附近影院失败：" + e.getMessage());
        }
    }

}