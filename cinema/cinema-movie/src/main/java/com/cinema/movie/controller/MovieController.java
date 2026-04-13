package com.cinema.movie.controller;

import com.cinema.common.entity.BaseResponse;
import com.cinema.movie.dto.MovieGenreDTO;
import com.cinema.movie.entity.Genre;
import com.cinema.movie.entity.Movie;
import com.cinema.movie.services.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/movie")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @GetMapping("/public/hot/list")
    public BaseResponse getHotMovieList(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return movieService.getHotMovies(pageNum, pageSize);
    }

    @GetMapping("/public/detail/{movieId}")
    public BaseResponse getMovieDetail(@PathVariable("movieId") Integer movieId) {
        return movieService.getMovieDetail(movieId);
    }

    @GetMapping("/public/hot/count")
    public BaseResponse getHotMovieCount() {
        return movieService.getHotMovieCount();
    }

    @GetMapping("/public/wait/list")
    public BaseResponse getWaitMovieList(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return movieService.getWaitMovies(pageNum, pageSize);
    }


    @PostMapping("/admin/add")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse addMovie(
            @RequestPart("movie") String movieJson,
            @RequestPart("genres") String genresJson,
            @RequestPart(value = "posterFile", required = false) MultipartFile posterFile,
            @RequestPart(value = "trailerFile", required = false) MultipartFile trailerFile) {

        try {
            // 解析JSON
            ObjectMapper objectMapper = new ObjectMapper();
            Movie movie = objectMapper.readValue(movieJson, Movie.class);
            List<Genre> genres = objectMapper.readValue(genresJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Genre.class));

            // 构建DTO
            MovieGenreDTO dto = new MovieGenreDTO();
            dto.setMovie(movie);
            dto.setGenres(genres);
            dto.setPosterFile(posterFile);
            dto.setTrailerFile(trailerFile);

            return movieService.addMovie(dto);

        } catch (Exception e) {
            log.error("添加电影失败", e);
            return BaseResponse.error(403,"请求参数解析失败: " );
        }
    }

    @PostMapping("/admin/changeState")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse changeState(Integer movieId, Integer state) {
        return movieService.changeState(movieId, state);
    }


    @PostMapping("/admin/changeMovie")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse changeMovie(
            @RequestPart("movie") String movieJson,
            @RequestPart("genres") String genresJson,
            @RequestPart(value = "posterFile", required = false) MultipartFile posterFile,
            @RequestPart(value = "trailerFile", required = false) MultipartFile trailerFile,
            @RequestPart(value = "oldPosterUrl", required = false) String oldPosterUrl,
            @RequestPart(value = "oldTrailerUrl", required = false) String oldTrailerUrl) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Movie movie = objectMapper.readValue(movieJson, Movie.class);
            List<Genre> genres = objectMapper.readValue(genresJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Genre.class));

            MovieGenreDTO dto = new MovieGenreDTO();
            dto.setMovie(movie);
            dto.setGenres(genres);
            dto.setPosterFile(posterFile);
            dto.setTrailerFile(trailerFile);
            dto.setOldPosterUrl(oldPosterUrl);
            dto.setOldTrailerUrl(oldTrailerUrl);

            return movieService.changeMovie(dto);

        } catch (Exception e) {
            log.error("修改电影失败", e);
            return BaseResponse.error(403,"请求参数解析失败: ");
        }
    }

    @GetMapping("/public/getAllMovie")
    public BaseResponse getAllMovie(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return movieService.getAllMovie(pageNum, pageSize);
    }

    @GetMapping("/public/find/ByName")
    public BaseResponse findByName(
            @RequestParam("name") String name,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return movieService.findByName(name, pageNum, pageSize);
    }

    @GetMapping("/public/find/ByGenre")
    public BaseResponse findByGenre(
            @RequestParam("genre") String genre,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return movieService.findByGenre(genre, pageNum, pageSize);
    }

    @DeleteMapping("/admin/delete/{movieId}")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse deleteMovie(@PathVariable("movieId") Integer movieId) {
        return movieService.deleteMovie(movieId);
    }

}