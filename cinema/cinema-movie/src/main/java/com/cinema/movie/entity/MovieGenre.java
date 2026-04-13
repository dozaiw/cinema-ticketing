package com.cinema.movie.entity;

import java.io.Serializable;

/**
 * 影片类型关联表(MovieGenre)实体类
 *
 * @author makejava
 * @since 2026-01-27 16:57:43
 */
public class MovieGenre implements Serializable {
    private static final long serialVersionUID = 745264327447515009L;

    private Integer movieId;

    private Integer genreId;


    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public Integer getGenreId() {
        return genreId;
    }

    public void setGenreId(Integer genreId) {
        this.genreId = genreId;
    }

}

