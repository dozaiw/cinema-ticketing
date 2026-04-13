package com.cinema.hall.services;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cinema.hall.entity.Hall;

import java.util.List;

/**
 * 影厅表(Hall)表服务接口
 *
 * @author makejava
 * @since 2026-01-30 20:25:01
 */
public interface HallService extends IService<Hall> {

    List<Hall> getCinemaHalls(Integer cinemaId);

    void addHall(Integer cinemaId, Hall hall);

    void updateHall(Hall hall);

    void  deleteHall(Integer hallId);

}
