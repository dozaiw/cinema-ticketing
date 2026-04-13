package com.cinema.movie.controller;

import com.cinema.auth.util.UserContextUtil;
import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.ResultCode;
import com.cinema.movie.entity.MovieFavorite;
import com.cinema.movie.services.MovieFavoriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@Slf4j
@RequestMapping("/movieFavorite")
public class MovieFavoritesController {

   @Autowired
   private MovieFavoriteService movieFavoriteService;
   @Autowired
   private UserContextUtil userContextUtil;

   @PostMapping("/addMovieFavorite/{movieId}")
    public BaseResponse addMovieFavorite(@PathVariable("movieId") Integer movieId) {
       Integer userId = userContextUtil.getUserId();
       MovieFavorite movieFavorite = new MovieFavorite();
       movieFavorite.setMovieId(movieId);
       movieFavorite.setUserId(userId);
       movieFavorite.setCreateTime(new java.util.Date());
       movieFavorite.setUpdateTime(new java.util.Date());
       return movieFavoriteService.addMovieFavorite(movieFavorite);
   }

   @PostMapping("/deleteMovieFavorite/{movieId}")
    public BaseResponse deleteMovieFavorite(@PathVariable("movieId") Integer movieId) {
       MovieFavorite movieFavorite = new MovieFavorite();
       movieFavorite.setMovieId(movieId);
       Integer userId = userContextUtil.getUserId();
       movieFavorite.setUserId(userId);
       return movieFavoriteService.deleteMovieFavorite(movieFavorite);
   }

   @GetMapping("/queryMovieFavorite")
    public BaseResponse queryMovieFavorite() {
       Integer currentUserId = userContextUtil.getUserId();
       try {
          return movieFavoriteService.queryMovieFavorite(currentUserId);
       }catch (Exception e){
          log.error("获取失败",e);
       }
       return BaseResponse.error(405,"获取失败");
   }

   // 查询用户某电影的收藏状态
   @GetMapping("/queryMovieFavoriteStatus/{movieId}")
    public BaseResponse queryMovieFavoriteStatus(@PathVariable("movieId") Integer movieId) {
      Integer userId = userContextUtil.getUserId();
      return movieFavoriteService.queryMovieFavoriteStatus(userId, movieId);
   }
}
