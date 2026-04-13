package com.cinema.movie.dto;

import lombok.Data;

@Data
public class CommentReportRequest {
    private Long commentId;
    private String reasonType;
    private String reportContent;
}
