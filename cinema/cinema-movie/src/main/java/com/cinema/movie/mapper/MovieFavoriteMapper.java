package com.cinema.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cinema.movie.entity.MovieFavorite;
import com.cinema.movie.vo.MovieFavoriteVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

public interface MovieFavoriteMapper extends BaseMapper<MovieFavorite> {
    List<MovieFavoriteVO> selectFavoriteList(int userId);
}
