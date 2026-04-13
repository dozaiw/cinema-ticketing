// com.cinema.movie.vo.MovieFavoriteVO.java
package com.cinema.movie.vo;

import com.cinema.movie.entity.Movie;
import lombok.Data;
import java.util.List;

@Data
public class MovieFavoriteVO {
    /**
     * 用户 ID
     */
    private Integer userId;

    /**
     * 收藏的电影列表
     */
    private List<Movie> movies;

    /**
     * 总数（用于分页）
     */
    private Long total;
}