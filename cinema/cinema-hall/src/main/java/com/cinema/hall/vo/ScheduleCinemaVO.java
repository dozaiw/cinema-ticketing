package com.cinema.hall.vo;

import com.cinema.hall.entity.Cinema;
import com.cinema.hall.entity.Schedule;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class ScheduleCinemaVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // 影院信息
    private Integer id;
    private String name;
    private String address;
    private String phone;
    private String city;
    private String district;
    
    // 最低价格
    private Double minPrice;
    
    // 排片列表
    private List<Schedule> schedules;

    public static ScheduleCinemaVO fromCinema(Cinema cinema, List<Schedule> schedules, Double minPrice) {
        ScheduleCinemaVO vo = new ScheduleCinemaVO();
        vo.setId(cinema.getId());
        vo.setName(cinema.getName());
        vo.setAddress(cinema.getAddress());
        vo.setPhone(cinema.getPhone());
        vo.setCity(cinema.getCity());
        vo.setDistrict(cinema.getDistrict());
        vo.setMinPrice(minPrice);
        vo.setSchedules(schedules);
        return vo;
    }
}