package com.cinema.movie.vo;

import lombok.Data;

import java.util.Date;

@Data
public class CommentReportVO {
    private Long id;
    private Long commentId;
    private Integer reporterId;
    private String reporterNickname;
    private String reasonType;
    private String reportContent;
    private Integer reportStatus;
    private Date reportTime;
    private String adminRemark;
    private Date handleTime;
    private Integer handledBy;

    private Integer movieId;
    private String movieTitle;
    private Integer commentStatus;
    private String commentContent;
    private String commentAuthorNickname;
}
