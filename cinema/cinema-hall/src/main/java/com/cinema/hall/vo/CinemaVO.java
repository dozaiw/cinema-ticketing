package com.cinema.hall.vo;

import lombok.Data;
import java.util.Date;

@Data
public class CinemaVO {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String city;
    private String district;
    private Double latitude;
    private Double longitude;
    private Integer status;
    private Date createTime;
    private Double distance;
}