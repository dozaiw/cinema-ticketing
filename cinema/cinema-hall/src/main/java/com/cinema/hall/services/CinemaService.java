package com.cinema.hall.services;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cinema.common.entity.BaseResponse;
import com.cinema.hall.dto.CinemaDTO;
import com.cinema.hall.entity.Cinema;
import com.cinema.hall.vo.NearbyCinemaVO;

import java.util.List;


/**
 * 影院服务接口
 */
public interface CinemaService extends IService<Cinema> {

    BaseResponse add(CinemaDTO cinemaDTO);

    BaseResponse update(CinemaDTO cinemaDTO);

    BaseResponse changeStatus(Long id, Integer status);

    BaseResponse delete(Long id);

    BaseResponse listPage(Integer pageNum, Integer pageSize, String name, String city, Integer status);

    BaseResponse getById(Long id);

    List<NearbyCinemaVO> getNearbyCinemas(Double latitude, Double longitude);

    BaseResponse findByCity(String city);

    BaseResponse geocodeAddress(String address);

    BaseResponse getWorkingCinemaCount();
}