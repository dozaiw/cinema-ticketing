package com.cinema.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cinema.movie.entity.Actor;
import org.apache.ibatis.annotations.Mapper;

/**
 * 演员信息表(Actor)Mapper
 *
 * @author makejava
 * @since 2026-02-15 10:17:31
 */
@Mapper
public interface ActorMapper extends BaseMapper<Actor> {

}