package com.cinema.hall.services.impl;

import cn.hutool.http.HttpUtil;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cinema.hall.dto.LocationDTO;
import com.cinema.hall.services.MapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class MapServiceImpl implements MapService {

    @Value("${map.amap.key}")
    private String apiKey;

    @Value("${map.amap.secret}")
    private String secret;

    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";

    @Override
    public LocationDTO geocode(String address) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("key", apiKey);
        params.put("address", address);
        params.put("output", "JSON");

        String response = HttpUtil.get(GEOCODE_URL, params);
        log.debug("地理编码响应：{}", response);

        JSONObject json = JSON.parseObject(response);

        if ("1".equals(json.getString("status")) && json.getIntValue("count") > 0) {
            JSONObject geocode = json.getJSONArray("geocodes").getJSONObject(0);
            String location = geocode.getString("location");
            String[] lngLat = location.split(",");

            LocationDTO dto = new LocationDTO();
            dto.setLongitude(Double.parseDouble(lngLat[0]));
            dto.setLatitude(Double.parseDouble(lngLat[1]));
            dto.setFormattedAddress(geocode.getString("formatted_address"));

            return dto;
        }

        throw new Exception("地址解析失败：" + json.getString("info"));
    }

    @Override
    public String reverseGeocode(Double latitude, Double longitude) throws Exception {
        // 类似实现，略
        return null;
    }
}