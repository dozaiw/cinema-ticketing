package com.cinema.movie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import java.util.Date;
import java.io.Serializable;

/**
 * 演员信息表(Actor)实体类
 *
 * @author makejava
 * @since 2026-02-15 10:17:31
 */
public class Actor implements Serializable {
    private static final long serialVersionUID = -29013329713755374L;
/**
     * 演员ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;
/**
     * 演员姓名
     */
    private String name;
/**
     * 头像URL
     */
    private String avatarUrl;
/**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;


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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

}

