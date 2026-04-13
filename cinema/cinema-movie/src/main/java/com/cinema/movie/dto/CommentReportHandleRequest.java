package com.cinema.movie.dto;

import lombok.Data;

@Data
public class CommentReportHandleRequest {
    private Long reportId;
    /**
     * 1: 驳回举报并保留评论
     * 2: 确认违规并删除评论
     */
    private Integer action;
    private String adminRemark;
}
