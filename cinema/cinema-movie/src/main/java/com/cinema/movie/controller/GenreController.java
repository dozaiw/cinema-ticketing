package com.cinema.movie.controller;

import com.cinema.common.entity.BaseResponse;
import com.cinema.movie.dto.GenreDTO;
import com.cinema.movie.services.GenreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/genre")
public class GenreController {

    @Autowired
    private GenreService genreService;

    /**
     * 获取所有类型
     */
    @GetMapping("/list")
    public BaseResponse getAllGenres() {
        return genreService.getAllGenres();
    }

    /**
     * 添加类型
     */
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse addGenre(@RequestBody GenreDTO genreDTO) {
        return genreService.addGenre(genreDTO);
    }

    /**
     * 修改类型
     */
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse updateGenre(@RequestBody GenreDTO genreDTO) {
        return genreService.updateGenre(genreDTO);
    }

    /**
     * 删除类型
     */
    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse deleteGenre(@PathVariable("id") Integer id) {
        return genreService.deleteGenre(id);
    }

}