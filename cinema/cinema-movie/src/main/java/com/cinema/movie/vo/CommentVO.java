package com.cinema.movie.vo;

import lombok.Data;
import java.util.Date;

/**
 * 评论 VO（包含用户头像信息）
 */
@Data
public class CommentVO {
    
    /**
     * 评论 ID
     */
  private Long id;
    
    /**
     * 用户 ID
     */
  private Integer userId;
    
    /**
     * 用户昵称
     */
  private String nickname;
    
    /**
     * 用户头像 URL
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
}
