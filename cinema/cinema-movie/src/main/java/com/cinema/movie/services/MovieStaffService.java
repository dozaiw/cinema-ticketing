package com.cinema.movie.services;

import com.cinema.common.entity.BaseResponse;
import com.cinema.movie.entity.MovieStaff;

import java.util.List;

/**
 * 电影人员关联表(MovieStaff)服务接口
 *
 * @author makejava
 * @since 2026-02-15 10:17:31
 */
public interface MovieStaffService {

    /**
     * 添加电影人员关联
     */
    BaseResponse add(MovieStaff movieStaff);

    /**
     * 批量添加电影人员关联
     */
    BaseResponse batchAdd(List<MovieStaff> list);

    /**
     * 删除电影人员关联
     */
    BaseResponse delete(Integer id);

    /**
     * 根据电影ID和演员ID删除关联
     */
    BaseResponse deleteByMovieIdAndActorId(Integer movieId, Integer actorId);

    /**
     * 根据电影ID删除所有关联
     */
    BaseResponse deleteByMovieId(Integer movieId);

    /**
     * 修改电影人员关联
     */
    BaseResponse update(MovieStaff movieStaff);

    /**
     * 根据ID查询
     */
    BaseResponse getById(Integer id);

    /**
     * 根据电影ID查询所有人员（带演员信息）
     */
    BaseResponse getByMovieId(Integer movieId);

    /**
     * 根据演员ID查询所有参与的电影（带电影信息）
     */
    BaseResponse getByActorId(Integer actorId);

    /**
     * 根据电影ID和职务查询
     */
    BaseResponse getByMovieIdAndRole(Integer movieId, String role);

    /**
     * 查询所有关联
     */
    BaseResponse listAll();
}