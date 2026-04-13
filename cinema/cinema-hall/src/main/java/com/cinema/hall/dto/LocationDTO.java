package com.cinema.hall.dto;

import lombok.Data;

@Data
public class LocationDTO {
    private Double latitude;
    private Double longitude;
    private String formattedAddress;
}