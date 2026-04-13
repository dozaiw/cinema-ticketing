const { request, BASE_URL } = require('../../utils/request');
const { getToken, getUserInfo } = require('../../utils/auth');

Page({
  data: {
    movie: {},
    staffList: [],
    isFavorite: false,  // 收藏状态
    statusText: '',      // 状态文本
    commentList: [],     // 评论列表
    commentContent: '',  // 评论内容
    commentRating: 5,    // 评论评分 (0.5-5)
    showCommentInput: false, // 是否显示评论输入框
    showReportInput: false,  // 是否显示举报输入框
    reportCommentId: null,
    reportTargetNickname: '',
    reportReasonType: '辱骂攻击',
    reportContent: '',
    reportReasonOptions: ['辱骂攻击', '广告营销', '色情低俗', '虚假信息', '其他']
  },

  onLoad(options) {
    const movieId = options.id;
    this.loadMovieDetail(movieId);
    this.checkFavorite(movieId);
    this.loadComments(movieId);
  },

  // 加载电影详情
  async loadMovieDetail(movieId) {
    wx.showLoading({ title: '加载中...' });

    try {
      const res = await request({
        url: `${BASE_URL.MOVIE}/movie/public/detail/${movieId}`,
        method: 'GET'
      });

      wx.hideLoading();

      if (res.code === 200 || res.code === "200") {
        const movie = res.data;
        const statusMap = {
          1: '正在热映',
          2: '即将上映',
          3: '已下架'
        };
        this.setData({
          movie: movie,
          staffList: movie.staffList || [],
          statusText: statusMap[movie.status] || '未知'
        });
      } else {
        wx.showToast({ title: res.msg || '加载失败', icon: 'none' });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '网络错误', icon: 'none' });
      console.error(err);
    }
  },

  // 检查是否已收藏
  async checkFavorite(movieId) {
    try {
      const token = wx.getStorageSync('token');
      if (!token) {
        this.setData({ isFavorite: false });
        return;
      }

      // 查询该电影的收藏状态
      const res = await request({
        url: `${BASE_URL.MOVIE}/movieFavorite/queryMovieFavoriteStatus/${movieId}`,
        method: 'GET'
      });

      if (res.code === 200 || res.code === "200") {
        const isFavorite = res.data === '已收藏';
        this.setData({ isFavorite });
      }
    } catch (err) {
      console.error('检查收藏状态失败:', err);
    }
  },

  // 切换收藏状态
  async toggleFavorite() {
    const movieId = this.data.movie.id;
    const token = wx.getStorageSync('token');

    if (!token) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '处理中...' });

    try {
      let res;
      const wasFavorite = this.data.isFavorite;

      if (wasFavorite) {
        // 取消收藏
        res = await request({
          url: `${BASE_URL.MOVIE}/movieFavorite/deleteMovieFavorite/${movieId}`,
          method: 'POST'
        });
      } else {
        // 添加收藏
        res = await request({
          url: `${BASE_URL.MOVIE}/movieFavorite/addMovieFavorite/${movieId}`,
          method: 'POST'
        });
      }

      wx.hideLoading();

      if (res.code === 200 || res.code === "200") {
        const newFavorite = !wasFavorite;
        this.setData({ isFavorite: newFavorite });
        wx.showToast({
          title: newFavorite ? '收藏成功' : '已取消收藏',
          icon: 'success'
        });
      } else {
        wx.showToast({ title: res.msg || '操作失败', icon: 'none' });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '网络错误', icon: 'none' });
      console.error(err);
    }
  },

  // 日期格式化
  formatDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },

  // 跳转购票
  goSchedule() {
    wx.navigateTo({
      url: `/pages/schedule/schedule?movieId=${this.data.movie.id}`
    });
  },

  // 加载评论列表
  async loadComments(movieId, pageNum = 1) {
    try {
      const res = await request({
        url: `${BASE_URL.MOVIE}/comment/public/movie/${movieId}`,
        method: 'GET',
        data: { pageNum, pageSize: 10 }
      });

      if (res.code === 200 || res.code === "200") {
        const newComments = res.data?.records || [];
        console.log('评论数据:', newComments);

        // 判断是否可以删除（自己的评论）
        const token = getToken();
        const userInfo = token ? getUserInfo() : null;
        const currentUserId = userInfo?.id || userInfo?.userId || null;

        const commentsWithPermission = newComments.map(comment => ({
          ...comment,
          canDelete: comment.userId === currentUserId,
          canReport: comment.userId !== currentUserId
        }));

        if (pageNum === 1) {
          this.setData({ commentList: commentsWithPermission });
        } else {
          this.setData({
            commentList: this.data.commentList.concat(commentsWithPermission)
          });
        }
        console.log('设置评论列表:', commentsWithPermission);
      }
    } catch (err) {
      console.error('加载评论失败:', err);
    }
  },

  // 显示评论输入框
  showCommentModal() {
    const token = getToken();
    if (!token) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    this.setData({ showCommentInput: true });
  },

  // 隐藏评论输入框
  hideCommentModal() {
    this.setData({
      showCommentInput: false,
      commentContent: ''
    });
  },

  // 评论内容输入
  onCommentInput(e) {
    this.setData({ commentContent: e.detail.value });
  },

  // 评分选择
  onRatingTap(e) {
    const { starIndex, isHalf } = e.currentTarget.dataset;
    let rating = starIndex;  // 整星部分

    // 如果是半星，加上0.5
    if (isHalf) {
      rating += 0.5;
    }

    // 最小0.5星，最大5星
    if (rating < 0.5) rating = 0.5;
    if (rating > 5) rating = 5;

    this.setData({ commentRating: rating });
  },

  // 提交评论
  async submitComment() {
    const content = this.data.commentContent.trim();
    if (!content) {
      wx.showToast({ title: '请输入评论内容', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '提交中...' });

    try {
      const res = await request({
        url: `${BASE_URL.MOVIE}/comment/add`,
        method: 'POST',
        data: {
          movieId: this.data.movie.id,
          commentContent: content,
          userRating: this.data.commentRating * 2  // 0.5-5 转换为 1-10
        }
      });

      wx.hideLoading();

      if (res.code === 200 || res.code === "200") {
        wx.showToast({ title: '评论成功', icon: 'success' });
        this.hideCommentModal();
        // 重新加载评论列表
        this.loadComments(this.data.movie.id);
      } else {
        wx.showToast({ title: res.msg || '评论失败', icon: 'none' });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '网络错误', icon: 'none' });
      console.error(err);
    }
  },

  // 删除评论
  async deleteComment(e) {
    const { commentId } = e.currentTarget.dataset;
    const token = getToken();
    if (!token) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    wx.showModal({
      title: '确认删除',
      content: '确定要删除这条评论吗？',
      success: async (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '删除中...' });

          try {
            const delRes = await request({
              url: `${BASE_URL.MOVIE}/comment/user/delete/${commentId}`,
              method: 'DELETE'
            });

            wx.hideLoading();

            if (delRes.code === 200 || delRes.code === "200") {
              wx.showToast({ title: '删除成功', icon: 'success' });
              // 重新加载评论列表
              this.loadComments(this.data.movie.id);
            } else {
              wx.showToast({ title: delRes.msg || '删除失败', icon: 'none' });
            }
          } catch (err) {
            wx.hideLoading();
            wx.showToast({ title: '网络错误', icon: 'none' });
            console.error(err);
          }
        }
      }
    });
  },

  // 显示举报输入框
  openReportModal(e) {
    const token = getToken();
    if (!token) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    const { commentId, nickname } = e.currentTarget.dataset;
    this.setData({
      showReportInput: true,
      reportCommentId: commentId,
      reportTargetNickname: nickname || '该评论',
      reportReasonType: this.data.reportReasonOptions[0],
      reportContent: ''
    });
  },

  // 隐藏举报输入框
  hideReportModal() {
    this.setData({
      showReportInput: false,
      reportCommentId: null,
      reportTargetNickname: '',
      reportReasonType: this.data.reportReasonOptions[0],
      reportContent: ''
    });
  },

  // 选择举报原因
  selectReportReason(e) {
    this.setData({
      reportReasonType: e.currentTarget.dataset.reason
    });
  },

  // 举报说明输入
  onReportInput(e) {
    this.setData({
      reportContent: e.detail.value
    });
  },

  // 提交举报
  async submitReport() {
    if (!this.data.reportCommentId) {
      wx.showToast({ title: '评论信息缺失', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '提交中...' });

    try {
      const res = await request({
        url: `${BASE_URL.MOVIE}/comment/report`,
        method: 'POST',
        data: {
          commentId: this.data.reportCommentId,
          reasonType: this.data.reportReasonType,
          reportContent: this.data.reportContent.trim()
        }
      });

      wx.hideLoading();

      if (res.code === 200 || res.code === "200") {
        wx.showToast({ title: '举报已提交', icon: 'success' });
        this.hideReportModal();
      } else {
        wx.showToast({ title: res.msg || '举报失败', icon: 'none' });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: err.msg || err.message || '举报失败', icon: 'none' });
      console.error('提交举报失败:', err);
    }
  },

  // 格式化时间
  formatCommentTime(timeStr) {
    if (!timeStr) return '';
    const date = new Date(timeStr);
    const now = new Date();
    const diff = now - date;

    // 小于1小时
    if (diff < 3600000) {
      const minutes = Math.floor(diff / 60000);
      return minutes < 1 ? '刚刚' : `${minutes}分钟前`;
    }

    // 小于24小时
    if (diff < 86400000) {
      const hours = Math.floor(diff / 3600000);
      return `${hours}小时前`;
    }

    // 小于7天
    if (diff < 604800000) {
      const days = Math.floor(diff / 86400000);
      return `${days}天前`;
    }

    // 显示具体日期
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${month}-${day}`;
  }
});
