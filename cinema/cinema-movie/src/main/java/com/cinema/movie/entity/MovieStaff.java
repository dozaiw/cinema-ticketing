package com.cinema.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * 电影人员关联表(MovieStaff)
 *
 * @author makejava
 * @since 2026-02-15 10:17:31
 */
@Data
@TableName("movie_staff")
public class MovieStaff implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 电影ID
     */
    private Integer movieId;

    /**
     * 人员ID
     */
    private Integer actorId;

    /**
     * 职务(导演/主演/配角/编剧等)
     */
    private String role;

    /**
     * 角色名(演员时填写)
     */
    private String characterName;

    /**
     * 排序权重(主演优先显示)
     */
    private Integer orderIndex;
}