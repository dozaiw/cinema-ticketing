package com.cinema.movie.services;

import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.PageResult;
import com.cinema.movie.dto.CommentReportHandleRequest;
import com.cinema.movie.dto.CommentReportRequest;
import com.cinema.movie.entity.Comment;
import com.cinema.movie.vo.CommentReportVO;
import com.cinema.movie.vo.CommentVO;

public interface CommentService {
    
    /**
     * 添加评论
     */
    BaseResponse addComment(Comment comment);
    
    /**
     * 删除评论（逻辑删除）- 用户删除自己的评论
     */
    BaseResponse deleteCommentByUser(Long commentId);
    
    /**
     * 删除评论（逻辑删除）- 管理员删除任意评论
     */
    BaseResponse deleteCommentByAdmin(Long commentId);
    
    /**
     * 根据状态查询评论（分页）
     * @param status 评论状态（0:审核中，1:正常，2:删除）
     * @param pageNum 页码
     * @param pageSize 每页数量
     */
    BaseResponse<PageResult<CommentVO>> getCommentsByStatus(Integer status, Integer pageNum, Integer pageSize);
    
    /**
     * 根据时间查询评论（最新在前，分页）
     * @param movieId 电影 ID（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     */
    BaseResponse<PageResult<CommentVO>> getCommentsByTime(Integer movieId, Integer pageNum, Integer pageSize);

    /**
     * 提交评论举报
     */
    BaseResponse reportComment(CommentReportRequest request);

    /**
     * 管理端分页查询评论举报
     */
    BaseResponse<PageResult<CommentReportVO>> getCommentReports(Integer reportStatus, String reasonType, Integer pageNum, Integer pageSize);

    /**
     * 管理端处理评论举报
     */
    BaseResponse handleCommentReport(CommentReportHandleRequest request);
}
