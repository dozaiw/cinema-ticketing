package com.cinema.movie.services;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cinema.common.entity.BaseResponse;
import com.cinema.movie.dto.MovieGenreDTO;
import com.cinema.movie.entity.Genre;
import com.cinema.movie.entity.Movie;

import java.util.List;

public interface MovieService extends IService<Movie> {

    BaseResponse getHotMovies(Integer pageNum, Integer pageSize);

    BaseResponse getWaitMovies(Integer pageNum, Integer pageSize);

    BaseResponse addMovie(MovieGenreDTO movieGenreDTO);

    BaseResponse changeMovie(MovieGenreDTO movieGenreDTO);

    BaseResponse changeState(Integer var1, Integer var2);

    BaseResponse getAllMovie(Integer pageNum, Integer pageSize);

    BaseResponse findByName(String name, Integer pageNum, Integer pageSize);

    BaseResponse findByGenre(String genre, Integer pageNum, Integer pageSize);

    BaseResponse getHotMovieCount();

    BaseResponse deleteMovie(Integer movieId);

    BaseResponse getMovieDetail(Integer movieId);
}
