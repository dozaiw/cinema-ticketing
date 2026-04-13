package com.cinema.hall.dto;

import lombok.Data;

@Data
public class CinemaDTO {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String city;
    private String district;
    private Double latitude;
    private Double longitude;
    private Integer status;
}