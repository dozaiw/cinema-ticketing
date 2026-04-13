package com.cinema.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cinema.movie.entity.Genre;
import org.apache.ibatis.annotations.Select;


public interface GenreMapper extends BaseMapper<Genre> {
    @Select("SELECT COUNT(*) FROM movie_genre WHERE genre_id = #{genreId}")
    Integer countMoviesByGenre(Integer genreId);
}
