package com.cinema.hall.services;


import com.cinema.hall.dto.LocationDTO;

public interface MapService {
    LocationDTO geocode(String address) throws Exception;
    String reverseGeocode(Double latitude, Double longitude) throws Exception;
}