package com.cinema.hall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.cinema.hall.entity.Cinema;
import com.cinema.hall.vo.CinemaVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CinemaMapper extends BaseMapper<Cinema> {

    @Select("""
        SELECT 
            c.*,
            ROUND(
                6371 * acos(
                    cos(radians(#{latitude})) * 
                    cos(radians(c.latitude)) * 
                    cos(radians(c.longitude) - radians(#{longitude})) + 
                    sin(radians(#{latitude})) * 
                    sin(radians(c.latitude))
                ) * 1000,
                2
            ) AS distance
        FROM cinema c
        WHERE c.status = 1
          AND c.latitude IS NOT NULL
          AND c.longitude IS NOT NULL
        HAVING distance <= #{radius}
        ORDER BY distance ASC
        LIMIT 50
    """)

    List<CinemaVO> selectNearbyCinemas(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radius") Integer radius
    );
}