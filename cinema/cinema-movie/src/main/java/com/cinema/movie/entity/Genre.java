package com.cinema.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;

/**
 * 电影类型表(Genre)实体类
 *
 * @author makejava
 * @since 2026-01-27 16:44:29
 */
public class Genre implements Serializable {
    private static final long serialVersionUID = -65880974130503233L;
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

