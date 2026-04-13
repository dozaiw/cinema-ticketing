const { request, BASE_URL } = require('../../utils/request');

Page({
  data: {
    favoriteList: [],
    loading: false
  },

  onLoad() {
    this.loadFavorites();
  },

  onShow() {
    this.loadFavorites();
  },

  // 加载收藏列表
  async loadFavorites() {
    const token = wx.getStorageSync('token');
    if (!token) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      setTimeout(() => {
        wx.switchTab({ url: '/pages/user/user' });
      }, 1500);
      return;
    }

    this.setData({ loading: true });

    try {
      const res = await request({
        url: `${BASE_URL.MOVIE}/movieFavorite/queryMovieFavorite`,
        method: 'GET'
      });

      this.setData({ loading: false });

      if (res.code === 200 || res.code === "200") {
        this.setData({ favoriteList: res.data || [] });
      } else {
        wx.showToast({ title: res.msg || '加载失败', icon: 'none' });
      }
    } catch (err) {
      this.setData({ loading: false });
      wx.showToast({ title: '网络错误', icon: 'none' });
      console.error(err);
    }
  },

  // 跳转详情
  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/detail/detail?id=${id}` });
  },

  // 跳转购票
  goBuy(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/schedule/schedule?movieId=${id}` });
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

  // 获取状态文本
  getStatusText(status) {
    const statusMap = {
      1: '正在热映',
      2: '即将上映',
      3: '已下架'
    };
    return statusMap[status] || '未知';
  },

  // 获取状态颜色
  getStatusColor(status) {
    const colorMap = {
      1: '#FF5722',
      2: '#FF9800',
      3: '#999999'
    };
    return colorMap[status] || '#999999';
  },

  // 取消收藏
  async toggleFavorite(e) {
    const movieId = e.currentTarget.dataset.id;
    const index = e.currentTarget.dataset.index;
    const token = wx.getStorageSync('token');

    if (!token) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    wx.showModal({
      title: '确认取消',
      content: '确定要取消收藏这部电影吗？',
      success: async (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '处理中...' });

          try {
            const response = await request({
              url: `${BASE_URL.MOVIE}/movieFavorite/deleteMovieFavorite/${movieId}`,
              method: 'POST'
            });

            wx.hideLoading();

            if (response.code === 200 || response.code === "200") {
              // 从列表中移除
              const favoriteList = this.data.favoriteList;
              favoriteList.splice(index, 1);
              this.setData({ favoriteList });
              wx.showToast({ title: '已取消收藏', icon: 'success' });
            } else {
              wx.showToast({ title: response.msg || '操作失败', icon: 'none' });
            }
          } catch (err) {
            wx.hideLoading();
            wx.showToast({ title: '网络错误', icon: 'none' });
            console.error(err);
          }
        }
      }
    });
  }
});
