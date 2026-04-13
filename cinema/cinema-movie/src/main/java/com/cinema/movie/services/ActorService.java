package com.cinema.movie.services;

import com.cinema.common.entity.BaseResponse;
import com.cinema.movie.dto.ActorDTO;
import com.cinema.movie.entity.Actor;

import java.util.List;

/**
 * 演员信息表(Actor)服务接口
 *
 * @author makejava
 * @since 2026-02-15 10:17:31
 */
public interface ActorService {

    /**
     * 添加演员（支持头像上传）
     */
    BaseResponse add(ActorDTO actorDTO);

    /**
     * 删除演员（级联删除关联）
     */
    BaseResponse delete(Integer id);

    /**
     * 修改演员（支持头像更新）
     */
    BaseResponse update(ActorDTO actorDTO);

    /**
     * 根据ID查询演员
     */
    BaseResponse getById(Integer id);

    /**
     * 根据姓名查询演员
     */
    BaseResponse get(String name);

    /**
     * 查询所有演员列表
     */
    BaseResponse listAll();

    /**
     * 分页查询演员
     */
    BaseResponse listPage(Integer pageNum, Integer pageSize);

    BaseResponse listPageFiltered(Integer pageNum, Integer pageSize, String name);
}