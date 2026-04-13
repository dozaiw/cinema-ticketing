package com.cinema.hall.services.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cinema.common.entity.BaseResponse;

import com.cinema.hall.dto.CinemaDTO;
import com.cinema.hall.entity.Cinema;
import com.cinema.hall.mapper.CinemaMapper;
import com.cinema.hall.mapper.HallMapper;
import com.cinema.hall.services.CinemaService;
import com.cinema.hall.services.MapService;
import com.cinema.hall.dto.LocationDTO;
import com.cinema.hall.util.LocationUtil;
import com.cinema.hall.vo.CinemaVO;
import com.cinema.hall.vo.NearbyCinemaVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 影院服务实现类
 */
@Slf4j
@Service
public class CinemaServiceImpl extends ServiceImpl<CinemaMapper, Cinema> implements CinemaService {

    @Autowired
    private CinemaMapper cinemaMapper;

    @Autowired
    private HallMapper hallMapper;

    @Autowired
    private MapService mapService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse add(CinemaDTO dto) {
        try {
            if (dto == null || dto.getName() == null || dto.getName().trim().isEmpty()) {
                return BaseResponse.error(403, "影院名称不能为空");
            }
            if (dto.getAddress() == null || dto.getAddress().trim().isEmpty()) {
                return BaseResponse.error(403, "影院地址不能为空");
            }

            LambdaQueryWrapper<Cinema> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cinema::getName, dto.getName());
            Long count = cinemaMapper.selectCount(wrapper);
            if (count > 0) {
                return BaseResponse.error(403, "影院名称已存在");
            }

            if (dto.getLatitude() == null || dto.getLongitude() == null) {
                try {
                    LocationDTO location = mapService.geocode(dto.getAddress());
                    dto.setLatitude(location.getLatitude());
                    dto.setLongitude(location.getLongitude());
                } catch (Exception e) {
                    log.error("地理编码失败", e);
                    return BaseResponse.error(403, "无法获取影院位置，请检查地址");
                }
            }

            Cinema cinema = new Cinema();
            BeanUtils.copyProperties(dto, cinema);
            cinema.setCreateTime(new Date());
            cinema.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);

            int result = cinemaMapper.insert(cinema);
            return result > 0 ? BaseResponse.success("添加成功", cinema)
                    : BaseResponse.error(403, "添加失败");
        } catch (Exception e) {
            log.error("添加影院异常", e);
            return BaseResponse.error(500, "系统异常：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse update(CinemaDTO dto) {
        // 1. 参数校验
        if (dto == null || dto.getId() == null) {
            return BaseResponse.error(403, "影院 ID 不能为空");
        }

        // 2. 检查影院是否存在
        Cinema existCinema = cinemaMapper.selectById(dto.getId());
        if (existCinema == null) {
            return BaseResponse.error(403, "影院不存在");
        }

        // 3. 创建更新对象
        Cinema cinema = new Cinema();
        cinema.setId(Math.toIntExact(dto.getId()));  // 显式设置 ID
        cinema.setName(dto.getName());
        cinema.setAddress(dto.getAddress());
        cinema.setPhone(dto.getPhone());
        cinema.setCity(dto.getCity());
        cinema.setDistrict(dto.getDistrict());
        cinema.setLatitude(dto.getLatitude());
        cinema.setLongitude(dto.getLongitude());

        // 4. 执行更新
        int result = cinemaMapper.updateById(cinema);

        if (result > 0) {
            log.info("更新影院成功：{}", dto.getId());
            return BaseResponse.success("更新成功", null);
        } else {
            log.error("更新影院失败：{}", dto.getId());
            return BaseResponse.error(403, "更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse changeStatus(Long id, Integer status) {
        try {
            if (id == null || id <= 0) {
                return BaseResponse.error(403, "影院 ID 不能为空");
            }

            Cinema cinema = cinemaMapper.selectById(id);
            if (cinema == null) {
                return BaseResponse.error(403, "影院不存在");
            }

            cinema.setStatus(status);
            int result = cinemaMapper.updateById(cinema);
            return result > 0 ? BaseResponse.success("操作成功", cinema)
                    : BaseResponse.error(403, "操作失败");
        } catch (Exception e) {
            log.error("修改影院状态异常", e);
            return BaseResponse.error(500, "系统异常：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse delete(Long id) {
        try {
            if (id == null || id <= 0) {
                return BaseResponse.error(403, "影院 ID 不能为空");
            }

            Cinema cinema = cinemaMapper.selectById(id);
            if (cinema == null) {
                return BaseResponse.error(403, "影院不存在");
            }

            LambdaQueryWrapper<com.cinema.hall.entity.Hall> hallWrapper = new LambdaQueryWrapper<>();
            hallWrapper.eq(com.cinema.hall.entity.Hall::getCinemaId, id);
            Long hallCount = hallMapper.selectCount(hallWrapper);
            if (hallCount > 0) {
                return BaseResponse.error(403,
                        String.format("该影院下存在 %d 个影厅，无法删除", hallCount));
            }

            int result = cinemaMapper.deleteById(id);
            Map<String, Object> data = new HashMap<>();
            data.put("cinemaId", id);
            data.put("deletedHalls", 0);
            return result > 0 ? BaseResponse.success("删除成功", data)
                    : BaseResponse.error(403, "删除失败");
        } catch (Exception e) {
            log.error("删除影院异常", e);
            return BaseResponse.error(500, "系统异常：" + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse listPage(Integer pageNum, Integer pageSize,
                                 String name, String city, Integer status) {
        try {
            pageNum = pageNum == null ? 1 : pageNum;
            pageSize = pageSize == null ? 10 : pageSize;

            Page<Cinema> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<Cinema> wrapper = new LambdaQueryWrapper<>();

            if (name != null && !name.trim().isEmpty()) {
                wrapper.like(Cinema::getName, name);
            }
            if (city != null && !city.trim().isEmpty()) {
                wrapper.eq(Cinema::getCity, city);
            }
            if (status != null) {
                wrapper.eq(Cinema::getStatus, status);
            }

            wrapper.orderByDesc(Cinema::getCreateTime);

            Page<Cinema> result = cinemaMapper.selectPage(page, wrapper);

            Map<String, Object> data = new HashMap<>();
            data.put("records", result.getRecords());
            data.put("total", result.getTotal());
            data.put("pageNum", pageNum);
            data.put("pageSize", pageSize);
            data.put("pages", result.getPages());

            return BaseResponse.success(data);
        } catch (Exception e) {
            log.error("分页查询影院异常", e);
            return BaseResponse.error(500, "系统异常：" + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse getById(Long id) {
        try {
            if (id == null || id <= 0) {
                return BaseResponse.error(403, "影院 ID 不能为空");
            }

            Cinema cinema = cinemaMapper.selectById(id);
            if (cinema == null) {
                return BaseResponse.error(403, "影院不存在");
            }

            LambdaQueryWrapper<com.cinema.hall.entity.Hall> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(com.cinema.hall.entity.Hall::getCinemaId, id);
            List<com.cinema.hall.entity.Hall> halls = hallMapper.selectList(wrapper);

            Map<String, Object> data = new HashMap<>();
            data.put("cinema", cinema);
            data.put("hallList", halls);

            return BaseResponse.success(data);
        } catch (Exception e) {
            log.error("查询影院详情异常", e);
            return BaseResponse.error(500, "系统异常：" + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse findByCity(String city) {
        try {
            if (city == null || city.trim().isEmpty()) {
                return BaseResponse.error(403, "城市不能为空");
            }

            LambdaQueryWrapper<Cinema> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cinema::getCity, city)
                    .eq(Cinema::getStatus, 1)
                    .isNotNull(Cinema::getLatitude)
                    .isNotNull(Cinema::getLongitude);

            List<Cinema> cinemas = cinemaMapper.selectList(wrapper);
            return BaseResponse.success(cinemas);
        } catch (Exception e) {
            log.error("按城市查询影院异常", e);
            return BaseResponse.error(500, "系统异常：" + e.getMessage());
        }
    }

    @Override
    public BaseResponse geocodeAddress(String address) {
        try {
            if (address == null || address.trim().isEmpty()) {
                return BaseResponse.error(403, "地址不能为空");
            }

            LocationDTO location = mapService.geocode(address);
            return BaseResponse.success(location);
        } catch (Exception e) {
            log.error("地址解析异常", e);
            return BaseResponse.error(403, "地址解析失败：" + e.getMessage());
        }
    }

    @Override
    public BaseResponse getWorkingCinemaCount() {
        try {
            return BaseResponse.success(cinemaMapper.selectCount(new LambdaQueryWrapper<Cinema>().eq(Cinema::getStatus, 1)));
        } catch (Exception e) {
            log.error("查询影院数量异常", e);
            return BaseResponse.error(500, "系统异常：" + e.getMessage());
        }
    }


    @Override
    public List<NearbyCinemaVO> getNearbyCinemas(Double latitude, Double longitude) {
        // 1. 查询所有营业中的影院
        List<Cinema> allCinemas = this.list(
                new LambdaQueryWrapper<Cinema>()
                        .eq(Cinema::getStatus, 1)
        );

        if (allCinemas == null || allCinemas.isEmpty()) {
            return List.of();
        }

        // 2. 转换为 VO 并计算距离
        List<NearbyCinemaVO> cinemaVOs = allCinemas.stream()
                .filter(cinema -> LocationUtil.isValidCoordinate(
                        cinema.getLatitude(), cinema.getLongitude()))
                .map(cinema -> {
                    // 计算距离
                    double distance = LocationUtil.calculateDistance(
                            latitude, longitude,
                            cinema.getLatitude(), cinema.getLongitude()
                    );
                    // 转换为 VO
                    return NearbyCinemaVO.fromCinema(cinema, distance);
                })
                .sorted((v1, v2) -> Double.compare(v1.getDistance(), v2.getDistance()))
                .collect(Collectors.toList());

        log.info("查询附近影院：当前位置 ({}, {}), 找到 {} 家影院",
                latitude, longitude, cinemaVOs.size());

        return cinemaVOs;
    }

    /**
     * 计算两点之间的距离（Haversine 公式）
     * @param lat1 起点纬度
     * @param lon1 起点经度
     * @param lat2 终点纬度
     * @param lon2 终点经度
     * @return 距离（米）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; // 地球半径（米）

        // 转换为弧度
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // 返回距离（米）
    }
}