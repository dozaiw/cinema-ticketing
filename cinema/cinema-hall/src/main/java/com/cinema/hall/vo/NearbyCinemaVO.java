package com.cinema.hall.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 附近影院 VO（包含动态计算的距离）
 * 不修改 Cinema 实体类，单独创建 VO
 */
@Data
public class NearbyCinemaVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // 影院基本信息
    private Integer id;
    private String name;
    private String address;
    private String phone;
    private String city;
    private String district;
    
    // 坐标信息
    private Double latitude;
    private Double longitude;
    
    // ✅ 动态计算的距离（米）
    private Double distance;
    
    /**
     * 从 Cinema 实体转换
     */
    public static NearbyCinemaVO fromCinema(Object cinema, Double distance) {
        NearbyCinemaVO vo = new NearbyCinemaVO();
        // 使用反射或手动拷贝（根据实际字段调整）
        vo.setId(((com.cinema.hall.entity.Cinema) cinema).getId());
        vo.setName(((com.cinema.hall.entity.Cinema) cinema).getName());
        vo.setAddress(((com.cinema.hall.entity.Cinema) cinema).getAddress());
        vo.setPhone(((com.cinema.hall.entity.Cinema) cinema).getPhone());
        vo.setCity(((com.cinema.hall.entity.Cinema) cinema).getCity());
        vo.setDistrict(((com.cinema.hall.entity.Cinema) cinema).getDistrict());
        vo.setLatitude(((com.cinema.hall.entity.Cinema) cinema).getLatitude());
        vo.setLongitude(((com.cinema.hall.entity.Cinema) cinema).getLongitude());
        vo.setDistance(distance);
        return vo;
    }
}