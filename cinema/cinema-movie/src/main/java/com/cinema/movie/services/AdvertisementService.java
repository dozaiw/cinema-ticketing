package com.cinema.movie.services;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cinema.movie.entity.Advertisement;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdvertisementService extends IService<Advertisement> {
    /**
     * 新增广告
     * @param advertisement 广告实体
     * @return 是否成功
     */
    boolean addAdvertisement(Advertisement advertisement , MultipartFile imageFile);

    /**
     * 删除广告（逻辑删除）
     * @param id 广告ID
     * @return 是否成功
     */
    boolean deleteAdvertisement(Long id);

    /**
     * 更新广告信息
     * @param advertisement 广告实体
     * @return 是否成功
     */
    boolean updateAdvertisement(Advertisement advertisement,MultipartFile imageFile,String oldImageUrl);

    /**
     * 根据ID查询广告
     * @param id 广告ID
     * @return 广告实体
     */
    Advertisement getAdvertisementById(Long id);

    /**
     * 查询所有启用的广告（按排序值降序）
     * @return 广告列表
     */
    List<Advertisement> listEnabledAdvertisements();

    /**
     * 分页查询广告（支持条件筛选）
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param title 广告标题（模糊查询）
     * @param status 广告状态
     * @return 分页结果
     */
    IPage<Advertisement> pageAdvertisements(Integer pageNum, Integer pageSize, String title, Integer status);
}
