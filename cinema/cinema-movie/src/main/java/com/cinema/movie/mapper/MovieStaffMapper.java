package com.cinema.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cinema.movie.entity.MovieStaff;

import com.cinema.movie.vo.MovieStaffVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 电影人员关联表(MovieStaff)Mapper
 *
 * @author makejava
 * @since 2026-02-15 10:17:31
 */
@Mapper
public interface MovieStaffMapper extends BaseMapper<MovieStaff> {

    /**
     * 根据演员ID删除所有关联记录
     */
    int deleteByActorId(@Param("actorId") Integer actorId);

    /**
     * 根据电影ID删除所有关联记录
     */
    int deleteByMovieId(@Param("movieId") Integer movieId);

    /**
     * 根据电影ID和演员ID删除关联记录
     */
    int deleteByMovieIdAndActorId(@Param("movieId") Integer movieId, @Param("actorId") Integer actorId);

    /**
     * 根据电影ID查询所有人员（带演员信息）
     */
    List<MovieStaffVO> selectByMovieId(@Param("movieId") Integer movieId);

    /**
     * 根据演员ID查询所有参与的电影（带电影信息）
     */
    List<MovieStaffVO> selectByActorId(@Param("actorId") Integer actorId);

    /**
     * 根据电影ID和职务查询人员
     */
    List<MovieStaffVO> selectByMovieIdAndRole(@Param("movieId") Integer movieId, @Param("role") String role);

    /**
     * 批量插入电影人员关联
     */
    int batchInsert(@Param("list") List<MovieStaff> list);

}