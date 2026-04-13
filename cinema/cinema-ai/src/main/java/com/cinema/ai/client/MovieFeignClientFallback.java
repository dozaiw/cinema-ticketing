// com.cinema.ai.feign.MovieFeignClientFallback.java
package com.cinema.ai.client;

import com.cinema.ai.entity.Genre;
import com.cinema.ai.entity.Movie;
import com.cinema.common.entity.BaseResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class MovieFeignClientFallback implements MovieFeignClient {

    @Override
    public BaseResponse<Movie> getMovieDetail(Integer movieId) {
        return BaseResponse.error(503, "电影服务暂时不可用");
    }

    @Override
    public BaseResponse<List<Movie>> getHotMovies(Integer pageNum, Integer pageSize) {
        return BaseResponse.success("降级返回", Collections.emptyList());
    }

    @Override
    public BaseResponse<List<Movie>> findMoviesByName(String name, Integer pageNum, Integer pageSize) {
        return BaseResponse.success("降级返回", Collections.emptyList());
    }

    @Override
    public BaseResponse<List<Movie>> findMoviesByGenre(String genre, Integer pageNum, Integer pageSize) {
        return BaseResponse.success("降级返回", Collections.emptyList());
    }

    @Override
    public BaseResponse<List<Movie>> getAllMovies(Integer pageNum, Integer pageSize) {
        return BaseResponse.success("降级返回", Collections.emptyList());
    }

    @Override
    public BaseResponse<List<Genre>> getAllGenres() {
        return BaseResponse.success("降级返回", Collections.emptyList());
    }

    @Override
    public BaseResponse<List<Movie>> getUserFavoriteMovies() {
        return BaseResponse.error(503, "收藏服务暂时不可用");
    }

    @Override
    public BaseResponse<Boolean> getFavoriteStatus(Integer movieId) {
        return BaseResponse.success("查询失败", false);
    }
}