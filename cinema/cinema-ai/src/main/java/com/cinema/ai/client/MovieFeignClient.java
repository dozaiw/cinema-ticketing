package com.cinema.ai.client;

import com.cinema.ai.config.FeignConfig;
import com.cinema.ai.entity.Genre;
import com.cinema.ai.entity.Movie;
import com.cinema.common.entity.BaseResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(
    name = "cinema-movie",
    url = "${feign.movie.url:http://localhost:8001}",
    fallback = MovieFeignClientFallback.class,
    configuration = FeignConfig.class
)
public interface MovieFeignClient {

    // ========== 电影查询 ==========
    
    @GetMapping("/movie/public/detail/{movieId}")
    BaseResponse getMovieDetail(@PathVariable("movieId") Integer movieId);

    @GetMapping("/movie/public/hot/list")
    BaseResponse getHotMovies(
        @RequestParam("pageNum") Integer pageNum,
        @RequestParam("pageSize") Integer pageSize
    );

    @GetMapping("/movie/public/find/ByName")
    BaseResponse findMoviesByName(
        @RequestParam("name") String name,
        @RequestParam("pageNum") Integer pageNum,
        @RequestParam("pageSize") Integer pageSize
    );

    @GetMapping("/movie/public/find/ByGenre")
    BaseResponse findMoviesByGenre(
        @RequestParam("genre") String genre,
        @RequestParam("pageNum") Integer pageNum,
        @RequestParam("pageSize") Integer pageSize
    );

    @GetMapping("/movie/public/getAllMovie")
    BaseResponse getAllMovies(
        @RequestParam("pageNum") Integer pageNum,
        @RequestParam("pageSize") Integer pageSize
    );

    // ========== 类型查询 ==========
    
    @GetMapping("/genre/list")
    BaseResponse getAllGenres();

    // ========== 收藏查询 ==========
    
    @GetMapping("/movieFavorite/queryMovieFavorite")
    BaseResponse getUserFavoriteMovies();

    @GetMapping("/movieFavorite/queryMovieFavoriteStatus/{movieId}")
    BaseResponse getFavoriteStatus(@PathVariable("movieId") Integer movieId);
}