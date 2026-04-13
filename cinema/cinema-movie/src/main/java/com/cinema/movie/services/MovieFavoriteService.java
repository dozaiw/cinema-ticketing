package com.cinema.movie.services;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cinema.common.entity.BaseResponse;
import com.cinema.movie.entity.MovieFavorite;

public interface MovieFavoriteService extends IService<MovieFavorite> {
    BaseResponse addMovieFavorite(MovieFavorite movieFavorite);
    BaseResponse deleteMovieFavorite(MovieFavorite movieFavorite);
    BaseResponse queryMovieFavorite(Integer userId);
    BaseResponse queryMovieFavoriteStatus(Integer userId, Integer movieId);
}
