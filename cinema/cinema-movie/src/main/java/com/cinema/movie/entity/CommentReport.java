package com.cinema.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("comment_report")
public class CommentReport implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long commentId;
    private Integer reporterId;
    private String reporterNickname;
    private String reasonType;
    private String reportContent;
    /**
     * 0: 待处理 1: 驳回举报 2: 评论已删除
     */
    private Integer reportStatus;
    private Date reportTime;
    private String adminRemark;
    private Date handleTime;
    private Integer handledBy;
}
