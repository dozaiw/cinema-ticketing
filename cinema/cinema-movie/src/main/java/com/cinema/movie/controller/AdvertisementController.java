package com.cinema.movie.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cinema.common.entity.BaseResponse;
import com.cinema.movie.entity.Advertisement;
import com.cinema.movie.services.AdvertisementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 广告控制器（RESTful 风格）
 * 接口前缀：/advertisement
 */
@RestController
@RequestMapping("/advertisement")
@Slf4j
public class AdvertisementController {

    @Autowired
    private AdvertisementService advertisementService;

    /**
     * 新增广告（支持文件上传）
     * Content-Type: multipart/form-data
     */
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse<Boolean> addAdvertisement(
            @RequestPart("advertisement") String advertisementJson,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        try {
            // 解析JSON参数
            ObjectMapper objectMapper = new ObjectMapper();
            Advertisement advertisement = objectMapper.readValue(advertisementJson, Advertisement.class);

            boolean result = advertisementService.addAdvertisement(advertisement, imageFile);
            return result ? BaseResponse.success(true) : BaseResponse.error(500, "新增广告失败");

        } catch (Exception e) {
            log.error("新增广告接口异常", e);
            return BaseResponse.error(500, "新增广告异常：" + e.getMessage());
        }
    }

    /**
     * 更新广告
     * Content-Type: multipart/form-data
     */
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse<Boolean> updateAdvertisement(
            @RequestPart("advertisement") String advertisementJson,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestPart(value = "oldImageUrl", required = false) String oldImageUrl) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Advertisement advertisement = objectMapper.readValue(advertisementJson, Advertisement.class);

            boolean result = advertisementService.updateAdvertisement(advertisement, imageFile, oldImageUrl);
            return result ? BaseResponse.success() : BaseResponse.error(500, "更新广告失败");

        } catch (Exception e) {
            log.error("更新广告接口异常", e);
            return BaseResponse.error(500, "更新广告异常：" + e.getMessage());
        }
    }

    /**
     * 删除广告
     */
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse<Boolean> deleteAdvertisement(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return BaseResponse.error(500,"广告ID不合法");
        }
        try {
            boolean result = advertisementService.deleteAdvertisement(id);
            if (result) {
                return BaseResponse.success(true);
            } else {
                return BaseResponse.error(500,"删除广告失败");
            }
        } catch (Exception e) {
            log.error("删除广告接口异常，ID：{}", id, e);
            return BaseResponse.error(500,"删除广告异常：");
        }
    }


    /**
     * 根据ID查询广告
     */
    @GetMapping("/get/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse<Advertisement> getAdvertisementById( @PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return BaseResponse.error(500,"广告ID不合法");
        }
        try {
            Advertisement advertisement = advertisementService.getAdvertisementById(id);
            if (advertisement != null) {
                return BaseResponse.success(advertisement);
            } else {
                return BaseResponse.error(500,"未查询到该广告");
            }
        } catch (Exception e) {
            log.error("查询广告接口异常，ID：{}", id, e);
            return BaseResponse.error(500,"查询广告异常：" + e.getMessage());
        }
    }

    /**
     * 查询所有启用的广告（首页轮播图用）
     */
    @GetMapping("/list/enabled")
    public BaseResponse<List<Advertisement>> listEnabledAdvertisements() {
        try {
            List<Advertisement> list = advertisementService.listEnabledAdvertisements();
            return BaseResponse.success(list);
        } catch (Exception e) {
            log.error("查询启用广告列表接口异常", e);
            return BaseResponse.error(500,"查询广告列表异常：" + e.getMessage());
        }
    }

    /**
     * 分页查询广告（后台管理用）
     */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse<IPage<Advertisement>> pageAdvertisements(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "status", required = false) Integer status) {
        try {
            IPage<Advertisement> page = advertisementService.pageAdvertisements(pageNum, pageSize, title, status);
            return BaseResponse.success(page);
        } catch (Exception e) {
            log.error("分页查询广告接口异常", e);
            return BaseResponse.error(500,"分页查询广告异常：" + e.getMessage());
        }
    }
}