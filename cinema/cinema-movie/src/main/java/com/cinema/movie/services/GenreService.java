package com.cinema.movie.services;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cinema.common.entity.BaseResponse;
import com.cinema.movie.dto.GenreDTO;
import com.cinema.movie.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 电影类型表(Genre)表服务接口
 *
 * @author makejava
 * @since 2026-01-27 16:44:30
 */
public interface GenreService {

    /**
     * 获取所有类型
     */
    BaseResponse getAllGenres();

    /**
     * 添加类型
     */
    BaseResponse addGenre(GenreDTO genreDTO);

    /**
     * 修改类型
     */
    BaseResponse updateGenre(GenreDTO genreDTO);

    /**
     * 删除类型
     */
    BaseResponse deleteGenre(Integer id);

}
