package com.cinema.movie.services.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cinema.auth.entity.User;
import com.cinema.auth.util.UserContextUtil;
import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.PageResult;
import com.cinema.common.util.BeanCopyUtil;
import com.cinema.movie.dto.CommentReportHandleRequest;
import com.cinema.movie.dto.CommentReportRequest;
import com.cinema.movie.entity.Comment;
import com.cinema.movie.entity.CommentReport;
import com.cinema.movie.entity.Movie;
import com.cinema.movie.mapper.CommentMapper;
import com.cinema.movie.mapper.CommentReportMapper;
import com.cinema.movie.mapper.MovieMapper;
import com.cinema.movie.services.CommentService;
import com.cinema.movie.vo.CommentReportVO;
import com.cinema.movie.vo.CommentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommentServiceImpl implements CommentService {
    private static final int COMMENT_STATUS_DELETED = 2;
    private static final int REPORT_STATUS_PENDING = 0;
    private static final int REPORT_STATUS_REJECTED = 1;
    private static final int REPORT_STATUS_COMMENT_DELETED = 2;

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private CommentReportMapper commentReportMapper;
    @Autowired
    private MovieMapper movieMapper;
    @Autowired
    private UserContextUtil userContextUtil;

    @Override
    @Transactional
    public BaseResponse addComment(Comment comment) {
        try {
            log.info("添加评论请求：userId={}, movieId={}", comment.getUserId(), comment.getMovieId());

            if (comment.getMovieId() == null || comment.getMovieId() <= 0) {
                return BaseResponse.error(403, "电影 ID 无效");
            }
            if (!StringUtils.hasText(comment.getCommentContent())) {
                return BaseResponse.error(403, "评论内容不能为空");
            }
            if (comment.getUserRating() == null || comment.getUserRating() < 0 || comment.getUserRating() > 10) {
                return BaseResponse.error(403, "评分应为 0-10 分");
            }

            Integer userId = null;
            String nickname = null;
            String userAvatar = null;
            try {
                User currentUser = userContextUtil.getCurrentUser();
                if (currentUser != null) {
                    userId = currentUser.getId();
                    nickname = StringUtils.hasText(currentUser.getNickname()) ? currentUser.getNickname() : currentUser.getUsername();
                    userAvatar = currentUser.getAvatar();
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败，使用传入的 userId");
                userId = comment.getUserId();
                nickname = StringUtils.hasText(comment.getNickname()) ? comment.getNickname() : "匿名用户";
                userAvatar = comment.getUserAvatar();
            }

            if (userId == null) {
                return BaseResponse.error(403, "用户未登录");
            }

            comment.setUserId(userId);
            comment.setNickname(nickname);
            comment.setUserAvatar(userAvatar);
            comment.setCommentContent(comment.getCommentContent().trim());
            comment.setCommentTime(new Date());
            comment.setCommentStatus(1);

            int result = commentMapper.insert(comment);
            if (result > 0) {
                log.info("添加评论成功：commentId={}, movieId={}", comment.getId(), comment.getMovieId());
                return BaseResponse.success("评论提交成功", comment);
            }
            return BaseResponse.error(403, "添加失败");
        } catch (Exception e) {
            log.error("添加评论异常", e);
            return BaseResponse.error(500, "系统异常：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public BaseResponse deleteCommentByUser(Long commentId) {
        try {
            log.info("用户删除评论请求：commentId={}", commentId);
            if (commentId == null || commentId <= 0) {
                return BaseResponse.error(403, "评论 ID 无效");
            }

            Comment comment = commentMapper.selectById(commentId);
            if (comment == null) {
                return BaseResponse.error(403, "评论不存在");
            }
            if (Objects.equals(comment.getCommentStatus(), COMMENT_STATUS_DELETED)) {
                return BaseResponse.error(403, "该评论已被删除");
            }

            User currentUser = userContextUtil.getCurrentUser();
            if (currentUser == null) {
                return BaseResponse.error(403, "用户未登录");
            }
            if (!currentUser.getId().equals(comment.getUserId())) {
                log.warn("用户尝试删除他人评论：userId={}, commentOwnerId={}", currentUser.getId(), comment.getUserId());
                return BaseResponse.error(403, "无权删除他人评论");
            }

            comment.setCommentStatus(COMMENT_STATUS_DELETED);
            int result = commentMapper.updateById(comment);
            if (result > 0) {
                resolvePendingReportsForDeletedComment(commentId, null, "评论已由发布者删除", new Date());
                log.info("用户删除评论成功：userId={}, commentId={}", currentUser.getId(), commentId);
                return BaseResponse.success("删除成功", null);
            }
            return BaseResponse.error(403, "删除失败");
        } catch (Exception e) {
            log.error("用户删除评论异常", e);
            return BaseResponse.error(500, "系统异常：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public BaseResponse deleteCommentByAdmin(Long commentId) {
        try {
            log.info("管理员删除评论请求：commentId={}", commentId);
            if (commentId == null || commentId <= 0) {
                return BaseResponse.error(403, "评论 ID 无效");
            }

            Comment comment = commentMapper.selectById(commentId);
            if (comment == null) {
                return BaseResponse.error(403, "评论不存在");
            }
            if (Objects.equals(comment.getCommentStatus(), COMMENT_STATUS_DELETED)) {
                return BaseResponse.error(403, "该评论已被删除");
            }

            User currentUser = userContextUtil.getCurrentUser();
            Integer adminId = currentUser != null ? currentUser.getId() : null;

            comment.setCommentStatus(COMMENT_STATUS_DELETED);
            int result = commentMapper.updateById(comment);
            if (result > 0) {
                resolvePendingReportsForDeletedComment(commentId, adminId, "管理员已删除评论", new Date());
                log.info("管理员删除评论成功：adminId={}, commentId={}", adminId, commentId);
                return BaseResponse.success("删除成功", null);
            }
            return BaseResponse.error(403, "删除失败");
        } catch (Exception e) {
            log.error("管理员删除评论异常", e);
            return BaseResponse.error(500, "系统异常：" + e.getMessage());
        }
    }

    @Override
    public BaseResponse<PageResult<CommentVO>> getCommentsByStatus(Integer status, Integer pageNum, Integer pageSize) {
        try {
            log.info("根据状态查询评论：status={}, pageNum={}, pageSize={}", status, pageNum, pageSize);
            pageNum = normalizePageNum(pageNum);
            pageSize = normalizePageSize(pageSize);

            LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
            if (status != null) {
                queryWrapper.eq(Comment::getCommentStatus, status);
            } else {
                queryWrapper.eq(Comment::getCommentStatus, 1);
            }
            queryWrapper.orderByDesc(Comment::getCommentTime);

            Page<Comment> result = commentMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
            PageResult<CommentVO> pageResult = new PageResult<>(buildCommentVOList(result.getRecords()), result.getTotal(), pageNum, pageSize);
            return BaseResponse.success(pageResult);
        } catch (Exception e) {
            log.error("根据状态查询评论异常", e);
            return BaseResponse.error(500, "系统异常：" + e.getMessage());
        }
    }

    @Override
    public BaseResponse<PageResult<CommentVO>> getCommentsByTime(Integer movieId, Integer pageNum, Integer pageSize) {
        try {
            log.info("根据时间查询评论：movieId={}, pageNum={}, pageSize={}", movieId, pageNum, pageSize);
            pageNum = normalizePageNum(pageNum);
            pageSize = normalizePageSize(pageSize);

            LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
            if (movieId != null && movieId > 0) {
                queryWrapper.eq(Comment::getMovieId, movieId);
            }
            queryWrapper.eq(Comment::getCommentStatus, 1);
            queryWrapper.orderByDesc(Comment::getCommentTime);

            Page<Comment> result = commentMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
            PageResult<CommentVO> pageResult = new PageResult<>(buildCommentVOList(result.getRecords()), result.getTotal(), pageNum, pageSize);
            return BaseResponse.success(pageResult);
        } catch (Exception e) {
            log.error("根据时间查询评论异常", e);
            return BaseResponse.error(500, "系统异常：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public BaseResponse reportComment(CommentReportRequest request) {
        try {
            if (request == null || request.getCommentId() == null || request.getCommentId() <= 0) {
                return BaseResponse.error(403, "评论 ID 无效");
            }
            if (!StringUtils.hasText(request.getReasonType())) {
                return BaseResponse.error(403, "请选择举报原因");
            }
            if (request.getReportContent() != null && request.getReportContent().length() > 200) {
                return BaseResponse.error(403, "补充说明不能超过 200 个字");
            }

            User currentUser = userContextUtil.getCurrentUser();
            if (currentUser == null) {
                return BaseResponse.error(403, "用户未登录");
            }

            Comment comment = commentMapper.selectById(request.getCommentId());
            if (comment == null || Objects.equals(comment.getCommentStatus(), COMMENT_STATUS_DELETED)) {
                return BaseResponse.error(403, "评论不存在或已被删除");
            }
            if (currentUser.getId().equals(comment.getUserId())) {
                return BaseResponse.error(403, "不能举报自己的评论");
            }

            LambdaQueryWrapper<CommentReport> duplicateWrapper = new LambdaQueryWrapper<>();
            duplicateWrapper.eq(CommentReport::getCommentId, request.getCommentId())
                    .eq(CommentReport::getReporterId, currentUser.getId())
                    .eq(CommentReport::getReportStatus, REPORT_STATUS_PENDING);
            Long duplicateCount = commentReportMapper.selectCount(duplicateWrapper);
            if (duplicateCount != null && duplicateCount > 0) {
                return BaseResponse.error(403, "您已举报过该评论，请等待审核");
            }

            CommentReport report = new CommentReport();
            report.setCommentId(request.getCommentId());
            report.setReporterId(currentUser.getId());
            report.setReporterNickname(StringUtils.hasText(currentUser.getNickname()) ? currentUser.getNickname() : currentUser.getUsername());
            report.setReasonType(request.getReasonType().trim());
            report.setReportContent(StringUtils.hasText(request.getReportContent()) ? request.getReportContent().trim() : null);
            report.setReportStatus(REPORT_STATUS_PENDING);
            report.setReportTime(new Date());

            int result = commentReportMapper.insert(report);
            if (result > 0) {
                log.info("评论举报提交成功：reportId={}, commentId={}, reporterId={}", report.getId(), report.getCommentId(), report.getReporterId());
                return BaseResponse.success("举报已提交，等待后台审核", report);
            }
            return BaseResponse.error(403, "举报提交失败");
        } catch (Exception e) {
            log.error("提交评论举报异常", e);
            return BaseResponse.error(500, "系统异常：" + e.getMessage());
        }
    }

    @Override
    public BaseResponse<PageResult<CommentReportVO>> getCommentReports(Integer reportStatus, String reasonType, Integer pageNum, Integer pageSize) {
        try {
            pageNum = normalizePageNum(pageNum);
            pageSize = normalizePageSize(pageSize);

            LambdaQueryWrapper<CommentReport> queryWrapper = new LambdaQueryWrapper<>();
            if (reportStatus != null) {
                queryWrapper.eq(CommentReport::getReportStatus, reportStatus);
            }
            if (StringUtils.hasText(reasonType)) {
                queryWrapper.eq(CommentReport::getReasonType, reasonType.trim());
            }
            queryWrapper.orderByDesc(CommentReport::getReportTime);

            Page<CommentReport> result = commentReportMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
            PageResult<CommentReportVO> pageResult = new PageResult<>(buildCommentReportVOList(result.getRecords()), result.getTotal(), pageNum, pageSize);
            return BaseResponse.success(pageResult);
        } catch (Exception e) {
            log.error("分页查询评论举报异常", e);
            return BaseResponse.error(500, "系统异常：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public BaseResponse handleCommentReport(CommentReportHandleRequest request) {
        try {
            if (request == null || request.getReportId() == null || request.getReportId() <= 0) {
                return BaseResponse.error(403, "举报 ID 无效");
            }
            if (request.getAction() == null || (request.getAction() != REPORT_STATUS_REJECTED && request.getAction() != REPORT_STATUS_COMMENT_DELETED)) {
                return BaseResponse.error(403, "处理动作无效");
            }
            if (request.getAdminRemark() != null && request.getAdminRemark().length() > 200) {
                return BaseResponse.error(403, "审核备注不能超过 200 个字");
            }

            CommentReport report = commentReportMapper.selectById(request.getReportId());
            if (report == null) {
                return BaseResponse.error(403, "举报记录不存在");
            }
            if (!Objects.equals(report.getReportStatus(), REPORT_STATUS_PENDING)) {
                return BaseResponse.error(403, "该举报已处理，请勿重复审核");
            }

            User currentUser = userContextUtil.getCurrentUser();
            Integer adminId = currentUser != null ? currentUser.getId() : null;
            Date now = new Date();
            String adminRemark = buildAdminRemark(request.getAction(), request.getAdminRemark());

            if (Objects.equals(request.getAction(), REPORT_STATUS_REJECTED)) {
                report.setReportStatus(REPORT_STATUS_REJECTED);
                report.setAdminRemark(adminRemark);
                report.setHandleTime(now);
                report.setHandledBy(adminId);
                commentReportMapper.updateById(report);
                return BaseResponse.success("已驳回举报", null);
            }

            Comment comment = commentMapper.selectById(report.getCommentId());
            if (comment != null && !Objects.equals(comment.getCommentStatus(), COMMENT_STATUS_DELETED)) {
                comment.setCommentStatus(COMMENT_STATUS_DELETED);
                commentMapper.updateById(comment);
            }
            resolvePendingReportsForDeletedComment(report.getCommentId(), adminId, adminRemark, now);
            return BaseResponse.success("已删除评论并处理相关举报", null);
        } catch (Exception e) {
            log.error("处理评论举报异常", e);
            return BaseResponse.error(500, "系统异常：" + e.getMessage());
        }
    }

    private List<CommentVO> buildCommentVOList(List<Comment> comments) {
        return BeanCopyUtil.copyList(comments, CommentVO.class);
    }

    private List<CommentReportVO> buildCommentReportVOList(List<CommentReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return Collections.emptyList();
        }

        List<CommentReportVO> reportVOList = BeanCopyUtil.copyList(reports, CommentReportVO.class);
        List<Long> commentIds = reports.stream()
                .map(CommentReport::getCommentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Comment> commentMap = commentIds.isEmpty()
                ? Collections.emptyMap()
                : commentMapper.selectBatchIds(commentIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Comment::getId, item -> item, (left, right) -> left));

        Set<Integer> movieIds = commentMap.values().stream()
                .map(Comment::getMovieId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Movie> movieMap = movieIds.isEmpty()
                ? Collections.emptyMap()
                : movieMapper.selectBatchIds(movieIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Movie::getId, item -> item, (left, right) -> left));

        reportVOList.forEach(reportVO -> {
            Comment comment = commentMap.get(reportVO.getCommentId());
            if (comment == null) {
                return;
            }
            reportVO.setMovieId(comment.getMovieId());
            reportVO.setCommentStatus(comment.getCommentStatus());
            reportVO.setCommentContent(comment.getCommentContent());
            reportVO.setCommentAuthorNickname(comment.getNickname());
            if (comment.getMovieId() != null) {
                Movie movie = movieMap.get(comment.getMovieId());
                if (movie != null) {
                    reportVO.setMovieTitle(movie.getTitle());
                }
            }
        });
        return reportVOList;
    }

    private void resolvePendingReportsForDeletedComment(Long commentId, Integer handledBy, String adminRemark, Date handleTime) {
        LambdaUpdateWrapper<CommentReport> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CommentReport::getCommentId, commentId)
                .eq(CommentReport::getReportStatus, REPORT_STATUS_PENDING)
                .set(CommentReport::getReportStatus, REPORT_STATUS_COMMENT_DELETED)
                .set(CommentReport::getAdminRemark, adminRemark)
                .set(CommentReport::getHandleTime, handleTime);
        if (handledBy != null) {
            updateWrapper.set(CommentReport::getHandledBy, handledBy);
        }
        commentReportMapper.update(null, updateWrapper);
    }

    private Integer normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private Integer normalizePageSize(Integer pageSize) {
        int normalized = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return Math.min(normalized, 50);
    }

    private String buildAdminRemark(Integer action, String adminRemark) {
        if (StringUtils.hasText(adminRemark)) {
            return adminRemark.trim();
        }
        return Objects.equals(action, REPORT_STATUS_REJECTED) ? "举报不成立，保留评论" : "评论违规，已删除";
    }
}
