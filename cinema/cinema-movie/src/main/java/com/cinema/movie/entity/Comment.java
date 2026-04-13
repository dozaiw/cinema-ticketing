package com.cinema.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.util.Date;
import java.io.Serializable;

/**
 * 电影评论表(Comment)实体类
 *
 * @author makejava
 * @since 2026-03-10 20:52:04
 */
public class Comment implements Serializable {
    private static final long serialVersionUID = 600500675069463475L;
/**
     * 评论 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
/**
     * 用户 ID
     */
    private Integer userId;
/**
     * 用户昵称 (快照)
     */
  private String nickname;
/**
 * 用户头像 URL (快照)
 */
  private String userAvatar;
/**
     * 评论时间
     */
    private Date commentTime;
/**
     * 评论状态 (0:审核中，1:正常，2:删除)
     */
    private Integer commentStatus;
/**
     * 用户评分
     */
    private Double userRating;
/**
     * 评论内容
     */
    private String commentContent;
/**
     * 电影 ID
     */
    private Integer movieId;


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

   public String getNickname() {
        return nickname;
    }

   public void setNickname(String nickname) {
        this.nickname = nickname;
    }

   public String getUserAvatar() {
        return userAvatar;
    }

   public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

   public Date getCommentTime() {
        return commentTime;
    }

    public void setCommentTime(Date commentTime) {
        this.commentTime = commentTime;
    }

    public Integer getCommentStatus() {
        return commentStatus;
    }

    public void setCommentStatus(Integer commentStatus) {
        this.commentStatus = commentStatus;
    }

    public Double getUserRating() {
        return userRating;
    }

    public void setUserRating(Double userRating) {
        this.userRating = userRating;
    }

    public String getCommentContent() {
        return commentContent;
    }

    public void setCommentContent(String commentContent) {
        this.commentContent = commentContent;
    }

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

}

