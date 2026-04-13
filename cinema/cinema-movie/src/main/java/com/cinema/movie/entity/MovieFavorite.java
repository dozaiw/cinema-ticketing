package com.cinema.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.util.Date;
import java.io.Serializable;

/**
 * 电影收藏表(MovieFavorite)实体类
 *
 * @author makejava
 * @since 2026-03-05 15:50:30
 */
public class MovieFavorite implements Serializable {
    private static final long serialVersionUID = 425722897677589429L;
/**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
/**
     * 用户 ID
     */
    private Integer userId;
/**
     * 电影 ID
     */
    private Integer movieId;
/**
     * 收藏时间
     */
    private Date createTime;
/**
     * 更新时间
     */
    private Date updateTime;
/**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer isDeleted;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

}

