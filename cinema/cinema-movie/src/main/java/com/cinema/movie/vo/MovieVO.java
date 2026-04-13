// src/main/java/com/cinema/movie/vo/MovieVO.java
package com.cinema.movie.vo;

import com.cinema.movie.entity.Genre;
import com.cinema.movie.entity.Movie;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class MovieVO {
    // 电影基本信息
    private Integer id;
    private String title;
    private String description;
    private Integer duration;
    private Date releaseDate;
    private Date offlineDate;
    private String poster;
    private String trailerUrl;
    private Integer status;
    private Date createTime;
    private Double score;

    // 演员列表
    private List<MovieStaffVO> staffList;

    private List<Genre> genres;

    // 从 Movie 实体转换
    public static MovieVO fromMovie(Movie movie) {
        MovieVO vo = new MovieVO();
        vo.setId(movie.getId());
        vo.setTitle(movie.getTitle());
        vo.setDescription(movie.getDescription());
        vo.setDuration(movie.getDuration());
        vo.setReleaseDate(movie.getReleaseDate());
        vo.setOfflineDate(movie.getOfflineDate());
        vo.setPoster(movie.getPoster());
        vo.setTrailerUrl(movie.getTrailerUrl());
        vo.setStatus(movie.getStatus());
        vo.setScore(movie.getScore());
        return vo;
    }
}