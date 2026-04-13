package com.cinema.movie.services.impl;

import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cinema.common.entity.BaseResponse;
import com.cinema.movie.entity.Movie;
import com.cinema.movie.entity.MovieFavorite;
import com.cinema.movie.mapper.MovieFavoriteMapper;
import com.cinema.movie.services.MovieFavoriteService;
import com.cinema.movie.vo.MovieFavoriteVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class MovieFavoriteServiceImpl extends ServiceImpl<MovieFavoriteMapper, MovieFavorite> implements MovieFavoriteService {

    @Autowired
    private MovieFavoriteMapper movieFavoriteMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    @Override
    public BaseResponse addMovieFavorite(MovieFavorite movieFavorite) {
        int result = movieFavoriteMapper.insert(movieFavorite);
        if (result > 0) {
            stringRedisTemplate.delete("movieFavorite:movies:" + movieFavorite.getUserId());
            return BaseResponse.success("添加成功");
        } else {
            return BaseResponse.error(405,"添加失败");
        }
    }

    @Transactional
    @Override
    public BaseResponse deleteMovieFavorite(MovieFavorite movieFavorite) {
        int result = movieFavoriteMapper
                .delete(new LambdaQueryWrapper<MovieFavorite>()
                        .eq(MovieFavorite::getUserId, movieFavorite.getUserId()).eq(MovieFavorite::getMovieId, movieFavorite.getMovieId()));
        if (result > 0) {
            stringRedisTemplate.delete("movieFavorite:movies:" + movieFavorite.getUserId());
            return BaseResponse.success("删除成功");
        } else {
            return BaseResponse.error(405,"删除失败");
        }
    }

    @Override
    public BaseResponse queryMovieFavorite(Integer userId) {
        try {
            //  缓存key
            String cacheKey = "movieFavorite:movies:" + userId;

            // 查缓存
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cachedJson)) {

                List<Movie> cachedList = objectMapper.readValue(
                        cachedJson,
                        new TypeReference<List<Movie>>() {}
                );
                return BaseResponse.success("查询成功(缓存)", cachedList);
            }

            List<MovieFavoriteVO> dbList = movieFavoriteMapper.selectFavoriteList(userId);

            if (!dbList.isEmpty()) {
                String json = objectMapper.writeValueAsString(dbList);
                stringRedisTemplate.opsForValue().set(cacheKey, json, 30, TimeUnit.MINUTES);
            } else {
                // 防缓存穿透
                stringRedisTemplate.opsForValue().set(cacheKey, "[]", 5, TimeUnit.MINUTES);
            }

            return BaseResponse.success("查询成功", dbList);

        } catch (JsonProcessingException e) {
            log.error("缓存序列化失败, userId={}", userId, e);
            return queryFromDb(userId);
        } catch (Exception e) {
            log.error("查询收藏异常, userId={}", userId, e);
            return BaseResponse.error(500, "系统繁忙，请稍后重试");
        }
    }

    // 降级方法也要改
    private BaseResponse queryFromDb(Integer userId) {
        try {
            // 改为 List<Movie>
            List<MovieFavoriteVO> list = movieFavoriteMapper.selectFavoriteList(userId);
            return BaseResponse.success("查询成功", list);
        } catch (Exception e) {
            log.error("降级查询失败", e);
            return BaseResponse.error(500, "查询失败");
        }
    }

    // 查询某电影，某用户的收藏状态
    @Override
    public BaseResponse queryMovieFavoriteStatus(Integer userId, Integer movieId) {
        MovieFavorite movieFavorite = movieFavoriteMapper.selectOne(new LambdaQueryWrapper<MovieFavorite>()
                .eq(MovieFavorite::getUserId, userId).eq(MovieFavorite::getMovieId, movieId));
        if (movieFavorite != null && movieFavorite.getIsDeleted() == 0) {
            return BaseResponse.success("已收藏");
        }
        return BaseResponse.success("未收藏");
    }

}
