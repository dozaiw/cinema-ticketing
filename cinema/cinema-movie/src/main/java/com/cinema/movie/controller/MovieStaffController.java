package com.cinema.movie.controller;

import com.cinema.common.entity.BaseResponse;
import com.cinema.movie.entity.MovieStaff;
import com.cinema.movie.services.MovieStaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 电影人员关联表(MovieStaff)控制层
 *
 * @author makejava
 * @since 2026-02-15 10:17:31
 */
@RestController
@RequestMapping("/movie-staff")
public class MovieStaffController {
    
    @Autowired
    private MovieStaffService movieStaffService;
    
    /**
     * 添加电影人员关联
     */
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse addMovieStaff(@RequestBody MovieStaff movieStaff) {
        return movieStaffService.add(movieStaff);
    }
    
    /**
     * 批量添加电影人员关联
     */
    @PostMapping("/batch-add")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse batchAddMovieStaff(@RequestBody List<MovieStaff> list) {
        return movieStaffService.batchAdd(list);
    }
    
    /**
     * 删除电影人员关联
     */
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse deleteMovieStaff(@PathVariable("id") Integer id) {
        return movieStaffService.delete(id);
    }
    
    /**
     * 根据电影ID和演员ID删除关联
     */
    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse deleteMovieStaffByMovieAndActor(
            @RequestParam("movieId") Integer movieId,
            @RequestParam("actorId") Integer actorId) {
        return movieStaffService.deleteByMovieIdAndActorId(movieId, actorId);
    }
    
    /**
     * 根据电影ID删除所有关联
     */
    @DeleteMapping("/delete-by-movie/{movieId}")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse deleteMovieStaffByMovieId(@PathVariable("movieId") Integer movieId) {
        return movieStaffService.deleteByMovieId(movieId);
    }
    
    /**
     * 修改电影人员关联
     */
    @PutMapping("/update")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse updateMovieStaff(@RequestBody MovieStaff movieStaff) {
        return movieStaffService.update(movieStaff);
    }
    
    /**
     * 根据ID查询
     */
    @GetMapping("/get/{id}")
    public BaseResponse getMovieStaffById(@PathVariable("id") Integer id) {
        return movieStaffService.getById(id);
    }
    
    /**
     * 根据电影ID查询所有人员（带演员信息）
     */
    @GetMapping("/movie/{movieId}")
    public BaseResponse getMovieStaffByMovieId(@PathVariable("movieId") Integer movieId) {
        return movieStaffService.getByMovieId(movieId);
    }
    
    /**
     * 根据演员ID查询所有参与的电影（带电影信息）
     */
    @GetMapping("/actor/{actorId}")
    public BaseResponse getMovieStaffByActorId(@PathVariable("actorId") Integer actorId) {
        return movieStaffService.getByActorId(actorId);
    }
    
    /**
     * 根据电影ID和职务查询
     */
    @GetMapping("/movie/{movieId}/role/{role}")
    public BaseResponse getMovieStaffByMovieIdAndRole(
            @PathVariable("movieId") Integer movieId,
            @PathVariable("role") String role) {
        return movieStaffService.getByMovieIdAndRole(movieId, role);
    }
    
    /**
     * 查询所有关联
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse listAllMovieStaff() {
        return movieStaffService.listAll();
    }
}