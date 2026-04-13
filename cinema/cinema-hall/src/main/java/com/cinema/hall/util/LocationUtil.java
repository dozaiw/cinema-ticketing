package com.cinema.hall.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 地理位置工具类
 * 提供距离计算、坐标校验等功能
 */
@Slf4j
public class LocationUtil {

    // 地球半径（米）
    private static final double EARTH_RADIUS = 6371000;

    /**
     * 计算两点之间的距离（Haversine 公式）
     * @param lat1 起点纬度
     * @param lon1 起点经度
     * @param lat2 终点纬度
     * @param lon2 终点经度
     * @return 距离（米）
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 转换为弧度
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * 校验经纬度是否有效
     * @param latitude 纬度
     * @param longitude 经度
     * @return true-有效，false-无效
     */
    public static boolean isValidCoordinate(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return latitude >= -90 && latitude <= 90 &&
               longitude >= -180 && longitude <= 180;
    }

    /**
     * 格式化距离显示
     * @param distanceMeters 距离（米）
     * @return 格式化后的字符串（如 "1.2km" 或 "500m"）
     */
    public static String formatDistance(double distanceMeters) {
        if (distanceMeters >= 1000) {
            return String.format("%.1fkm", distanceMeters / 1000);
        } else {
            return String.format("%.0fm", distanceMeters);
        }
    }
}