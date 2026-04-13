package com.cinema.movie.dto;

import com.cinema.movie.entity.Genre;
import com.cinema.movie.entity.Movie;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Data
public class MovieGenreDTO {
    // 要修改的电影对象
    private Movie movie;
    // 该电影关联的类型列表
    private List<Genre> genres;

    // 海报文件
    private MultipartFile posterFile;

    // 预告片文件
    private MultipartFile trailerFile;

    // 旧海报URL
    private String oldPosterUrl;

    // 旧预告片URL
    private String oldTrailerUrl;
}