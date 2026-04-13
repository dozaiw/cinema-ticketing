package com.cinema.movie.services.impl;

import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cinema.auth.util.CosUtil;
import com.cinema.movie.entity.Advertisement;
import com.cinema.movie.entity.Movie;
import com.cinema.movie.mapper.AdvertisementMapper;
import com.cinema.movie.mapper.MovieMapper;
import com.cinema.movie.services.AdvertisementService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Service
@Slf4j
public class AdvertisementServiceImp extends ServiceImpl<AdvertisementMapper, Advertisement> implements AdvertisementService {

    @Autowired
    private AdvertisementMapper advertisementMapper;
    @Autowired
    private CosUtil cosUtil;


    /**
     * 新增广告（支持图片上传）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addAdvertisement(Advertisement advertisement, MultipartFile imageFile) {
        try {
            // 1. 参数校验
            if (!StringUtils.hasText(advertisement.getTitle())) {
                log.error("新增广告失败：标题不能为空");
                return false;
            }

            // 2. 处理图片上传
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String imageUrl = cosUtil.uploadFile(imageFile, "advertisement");
                    advertisement.setImageUrl(imageUrl);
                    log.info("广告图上传成功：{}", imageUrl);
                } catch (Exception e) {
                    log.error("广告图上传失败", e);
                    return false;  // 上传失败则终止
                }
            } else if (!StringUtils.hasText(advertisement.getImageUrl())) {
                // 既没传文件，也没传已有URL
                log.error("新增广告失败：图片URL不能为空");
                return false;
            }

            // 3. 填充默认值
            if (advertisement.getStatus() == null) {
                advertisement.setStatus(1);
            }
            if (advertisement.getSortOrder() == null) {
                advertisement.setSortOrder(0);
            }
            advertisement.setIsDeleted(0);

            // 4. 持久化
            boolean save = save(advertisement);
            log.info("新增广告{}，广告ID：{}", save ? "成功" : "失败", advertisement.getId());
            return save;

        } catch (Exception e) {
            log.error("新增广告异常", e);
            // 🔥 事务回滚时清理已上传的COS文件（可选）
            if (advertisement.getImageUrl() != null) {
                try {
                    cosUtil.deleteFile(advertisement.getImageUrl());
                } catch (Exception ex) {
                    log.error("回滚时删除COS文件失败", ex);
                }
            }
            return false;
        }
    }

    /**
     * 更新广告（支持图片替换）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAdvertisement(Advertisement advertisement, MultipartFile imageFile, String oldImageUrl) {
        try {
            if (advertisement.getId() == null || advertisement.getId() <= 0) {
                log.error("更新广告失败：ID不能为空");
                return false;
            }

            // 1. 处理图片更新
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    // 上传新图
                    String newImageUrl = cosUtil.uploadFile(imageFile, "advertisement");
                    advertisement.setImageUrl(newImageUrl);

                    // 删除旧图（如果存在且是COS地址）
                    if (StringUtils.hasText(oldImageUrl) && !oldImageUrl.equals(newImageUrl)) {
                        cosUtil.deleteFile(oldImageUrl);
                        log.info("旧广告图已删除：{}", oldImageUrl);
                    }
                    log.info("广告图更新成功：{}", newImageUrl);
                } catch (Exception e) {
                    log.error("广告图更新失败", e);
                    return false;
                }
            }
            // 如果没传新图，保留原 imageUrl

            // 2. 校验核心参数
            if (!StringUtils.hasText(advertisement.getTitle())) {
                log.error("更新广告失败：标题不能为空");
                return false;
            }

            // 3. 执行更新（只更新非null字段）
            boolean update = updateById(advertisement);
            log.info("更新广告{}，广告ID：{}", update ? "成功" : "失败", advertisement.getId());
            return update;

        } catch (Exception e) {
            log.error("更新广告异常，ID：{}", advertisement.getId(), e);
            return false;
        }
    }

    /**
     * 删除广告（逻辑删除）
     */
    @Override
    public boolean deleteAdvertisement(Long id) {
        try {
            if (id == null || id <= 0) {
                log.error("删除广告失败：ID不合法");
                return false;
            }
            boolean update = advertisementMapper.deleteById(id) > 0;
            log.info("删除广告{}，广告ID：{}", update ? "成功" : "失败", id);
            return update;
        } catch (Exception e) {
            log.error("删除广告异常，ID：{}", id, e);
            return false;
        }
    }

    /**
     * 根据ID查询广告（排除已逻辑删除的）
     */
    @Override
    public Advertisement getAdvertisementById(Long id) {
        if (id == null || id <= 0) {
            log.warn("查询广告失败：ID不合法");
            return null;
        }
        // 构造查询条件：ID匹配 + 未删除
        LambdaQueryWrapper<Advertisement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Advertisement::getId, id)
                .eq(Advertisement::getIsDeleted, 0);
        return getOne(queryWrapper);
    }

    /**
     * 查询所有启用的广告（按排序值降序）
     */
    @Override
    public List<Advertisement> listEnabledAdvertisements() {
        LambdaQueryWrapper<Advertisement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Advertisement::getStatus, 1)        // 启用状态
                .eq(Advertisement::getIsDeleted, 0)      // 未删除
                .orderByDesc(Advertisement::getSortOrder); // 按排序值降序
        return list(queryWrapper);
    }

    /**
     * 分页查询广告（支持条件筛选）
     */
    @Override
    public IPage<Advertisement> pageAdvertisements(Integer pageNum, Integer pageSize, String title, Integer status) {
        // 分页参数校验
        if (pageNum == null || pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize <= 0 || pageSize > 100) {
            pageSize = 10; // 限制最大页大小
        }

        // 构造分页对象
        Page<Advertisement> page = new Page<>(pageNum, pageSize);

        // 构造查询条件
        LambdaQueryWrapper<Advertisement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Advertisement::getIsDeleted, 0); // 排除已删除的

        // 标题模糊查询
        if (StringUtils.hasText(title)) {
            queryWrapper.like(Advertisement::getTitle, title);
        }

        // 状态筛选
        if (status != null) {
            queryWrapper.eq(Advertisement::getStatus, status);
        }

        // 按更新时间降序
        queryWrapper.orderByDesc(Advertisement::getUpdateTime);

        // 执行分页查询
        return page(page, queryWrapper);
    }
}
