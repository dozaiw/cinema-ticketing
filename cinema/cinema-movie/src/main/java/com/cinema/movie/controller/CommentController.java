package com.cinema.movie.controller;

import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.PageResult;
import com.cinema.movie.dto.CommentReportHandleRequest;
import com.cinema.movie.dto.CommentReportRequest;
import com.cinema.movie.entity.Comment;
import com.cinema.movie.services.CommentService;
import com.cinema.movie.vo.CommentReportVO;
import com.cinema.movie.vo.CommentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 电影评论控制器
 */
@Slf4j
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
   private CommentService commentService;

    /**
     * 添加评论
     */
    @PostMapping("/add")
    @PreAuthorize("isAuthenticated()")
   public BaseResponse addComment(@RequestBody Comment comment) {
        try {
            log.info("添加评论请求：movieId={}, content={}", 
                    comment.getMovieId(), 
                    comment.getCommentContent() != null ? comment.getCommentContent().substring(0, Math.min(20, comment.getCommentContent().length())) : "");
            
            return commentService.addComment(comment);
            
        } catch (Exception e) {
            log.error("添加评论失败", e);
            return BaseResponse.error(500, "添加评论失败：" + e.getMessage());
        }
    }

    /**
     * 删除评论 - 用户删除自己的评论
     * 需要登录认证，只能删除自己发布的评论
     */
    @DeleteMapping("/user/delete/{commentId}")
    @PreAuthorize("isAuthenticated()")
  public BaseResponse deleteCommentByUser(@PathVariable("commentId") Long commentId) {
        try {
            log.info("用户删除评论请求：commentId={}", commentId);
            return commentService.deleteCommentByUser(commentId);
            
        } catch (Exception e) {
            log.error("用户删除评论失败", e);
            return BaseResponse.error(500, "删除评论失败：" + e.getMessage());
        }
    }

    /**
     * 删除评论 - 管理员删除任意评论
     * 需要 admin 权限，可以删除任何用户的评论
     */
    @DeleteMapping("/admin/delete/{commentId}")
    @PreAuthorize("hasAuthority('admin')")
  public BaseResponse deleteCommentByAdmin(@PathVariable("commentId") Long commentId) {
        try {
            log.info("管理员删除评论请求：commentId={}", commentId);
            return commentService.deleteCommentByAdmin(commentId);
            
        } catch (Exception e) {
            log.error("管理员删除评论失败", e);
            return BaseResponse.error(500, "删除评论失败：" + e.getMessage());
        }
    }

    /**
     * 根据状态查询评论（管理端）
     * 可用于审核评论、查看已删除评论等
     */
    @GetMapping("/admin/list/byStatus")
    @PreAuthorize("hasAuthority('admin')")
  public BaseResponse<PageResult<CommentVO>> getCommentsByStatus(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        
        try {
            log.info("根据状态查询评论：status={}, pageNum={}, pageSize={}", status, pageNum, pageSize);
            return commentService.getCommentsByStatus(status, pageNum, pageSize);
            
        } catch (Exception e) {
            log.error("根据状态查询评论失败", e);
            return BaseResponse.error(500, "查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据时间查询评论（最新在前，公开接口）
     * 支持按电影 ID 筛选
     */
    @GetMapping("/public/list/byTime")
  public BaseResponse<PageResult<CommentVO>> getCommentsByTime(
            @RequestParam(value = "movieId", required = false) Integer movieId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        
        try {
            log.info("根据时间查询评论：movieId={}, pageNum={}, pageSize={}", movieId, pageNum, pageSize);
            return commentService.getCommentsByTime(movieId, pageNum, pageSize);
            
        } catch (Exception e) {
            log.error("根据时间查询评论失败", e);
            return BaseResponse.error(500, "查询失败：" + e.getMessage());
        }
    }

    /**
     * 查询某电影的全部评论（按时间倒序）
     * 便捷接口，等同于 byTime 接口传入 movieId
     */
    @GetMapping("/public/movie/{movieId}")
    public BaseResponse<PageResult<CommentVO>> getMovieComments(
            @PathVariable("movieId") Integer movieId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        
        try {
            log.info("查询电影评论：movieId={}, pageNum={}, pageSize={}", movieId, pageNum, pageSize);
            return commentService.getCommentsByTime(movieId, pageNum, pageSize);
            
        } catch (Exception e) {
            log.error("查询电影评论失败", e);
            return BaseResponse.error(500, "查询失败：" + e.getMessage());
        }
    }

    /**
     * 用户举报评论
     */
    @PostMapping("/report")
    @PreAuthorize("isAuthenticated()")
    public BaseResponse reportComment(@RequestBody CommentReportRequest request) {
        try {
            log.info("提交评论举报请求：commentId={}, reasonType={}", request.getCommentId(), request.getReasonType());
            return commentService.reportComment(request);
        } catch (Exception e) {
            log.error("提交评论举报失败", e);
            return BaseResponse.error(500, "举报失败：" + e.getMessage());
        }
    }

    /**
     * 管理端分页查询评论举报
     */
    @GetMapping("/admin/report/list")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse<PageResult<CommentReportVO>> getCommentReports(
            @RequestParam(value = "reportStatus", required = false) Integer reportStatus,
            @RequestParam(value = "reasonType", required = false) String reasonType,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            log.info("查询评论举报列表：reportStatus={}, reasonType={}, pageNum={}, pageSize={}",
                    reportStatus, reasonType, pageNum, pageSize);
            return commentService.getCommentReports(reportStatus, reasonType, pageNum, pageSize);
        } catch (Exception e) {
            log.error("查询评论举报列表失败", e);
            return BaseResponse.error(500, "查询失败：" + e.getMessage());
        }
    }

    /**
     * 管理端处理评论举报
     */
    @PostMapping("/admin/report/handle")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse handleCommentReport(@RequestBody CommentReportHandleRequest request) {
        try {
            log.info("处理评论举报：reportId={}, action={}", request.getReportId(), request.getAction());
            return commentService.handleCommentReport(request);
        } catch (Exception e) {
            log.error("处理评论举报失败", e);
            return BaseResponse.error(500, "处理失败：" + e.getMessage());
        }
    }
}
