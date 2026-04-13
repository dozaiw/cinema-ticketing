package com.cinema.movie.services.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.ResultCode;
import com.cinema.movie.dto.GenreDTO;
import com.cinema.movie.entity.Genre;
import com.cinema.movie.mapper.GenreMapper;
import com.cinema.movie.services.GenreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 电影类型表(Genre)表服务实现类
 *
 * @author makejava
 * @since 2026-01-27 16:44:31
 */
@Service
@Slf4j
public class GenreServiceImpl implements GenreService {

    @Autowired
    private GenreMapper genreMapper;

    @Override
    public BaseResponse getAllGenres() {
        try {
            List<Genre> genres = genreMapper.selectList(new LambdaQueryWrapper<Genre>());
            return BaseResponse.success(genres);
        } catch (Exception e) {
            log.error("获取所有类型失败", e);
            return BaseResponse.error(ResultCode.USER_GET_ALL_GENRE_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse addGenre(GenreDTO genreDTO) {
        try {
            // 1. 校验类型名称是否已存在
            if (genreMapper.selectCount(new LambdaQueryWrapper<Genre>().eq(Genre::getName, genreDTO.getName())) > 0) {
                return BaseResponse.error(400, "类型名称已存在");
            }

            // 2. 添加类型
            Genre genre = new Genre();
            genre.setName(genreDTO.getName());
            genreMapper.insert(genre);

            log.info("类型添加成功: id={}, name={}", genre.getId(), genre.getName());
            return BaseResponse.success("添加成功");

        } catch (Exception e) {
            log.error("添加类型失败", e);
            return BaseResponse.error(ResultCode.USER_ADD_GENRE_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse updateGenre(GenreDTO genreDTO) {
        try {
            // 1. 检查类型是否存在
            Genre genre = genreMapper.selectById(genreDTO.getId());
            if (genre == null) {
                return BaseResponse.error(404, "类型不存在");
            }

            // 2. 校验类型名称是否已存在（排除自身）
            if (genreMapper.selectCount(
                    new LambdaQueryWrapper<Genre>()
                            .eq(Genre::getName, genreDTO.getName())
                            .ne(Genre::getId, genreDTO.getId())
            ) > 0) {
                return BaseResponse.error(400, "类型名称已存在");
            }

            // 3. 更新类型
            genre.setName(genreDTO.getName());
            genreMapper.updateById(genre);

            log.info("类型更新成功: id={}, name={}", genre.getId(), genre.getName());
            return BaseResponse.success("更新成功");

        } catch (Exception e) {
            log.error("更新类型失败", e);
            return BaseResponse.error(ResultCode.USER_UPDATE_GENRE_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse deleteGenre(Integer id) {
        try {
            // 1. 检查类型是否存在
            Genre genre = genreMapper.selectById(id);
            if (genre == null) {
                return BaseResponse.error(404, "类型不存在");
            }

            // 2. 检查类型是否被电影引用
            if (genreMapper.countMoviesByGenre(id) > 0) {
                return BaseResponse.error(400, "类型正在被电影使用，无法删除");
            }

            // 3. 删除类型
            genreMapper.deleteById(id);

            log.info("类型删除成功: id={}", id);
            return BaseResponse.success("删除成功");

        } catch (Exception e) {
            log.error("删除类型失败", e);
            return BaseResponse.error(ResultCode.USER_DELETE_GENRE_FAILED);
        }


    }


}
