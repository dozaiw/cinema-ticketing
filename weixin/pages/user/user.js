const { request, BASE_URL } = require('../../utils/request');
const { clearAuth, getToken, getUserInfo, isAdmin, saveUserInfo } = require('../../utils/auth');

Page({
  data: {
    userInfo: null,
    isLogin: false,
    isAdmin: false
  },

  onLoad() {
    this.loadUserInfo();
  },

  onShow() {
    this.loadUserInfo();
  },

  loadUserInfo() {
    const token = getToken();
    const userInfo = getUserInfo();

    if (token && userInfo) {
      this.setData({
        isLogin: true,
        userInfo,
        isAdmin: isAdmin(userInfo)
      });
    } else {
      this.setData({
        isLogin: false,
        isAdmin: false,
        userInfo: null
      });
    }
  },

  // 跳转到登录页面
  goToLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    });
  },

  // 微信登录（毕设用默认账号）
  async onGetPhoneNumber(e) {
    wx.showLoading({ title: '登录中...' });
    
    try {
      const testPhone = '13800138000';
      const testPassword = '123456';

      const res = await request({
        url: `${BASE_URL.AUTH}/auth/login`,
        method: 'POST',
        data: {
          username: testPhone,
          password: testPassword
        }
      });

      if (res.code === 200 || res.code === "200") {
        // 保存 Token
        const token = res.data.token || res.data;
        wx.setStorageSync('token', token);

        // 调用获取用户信息接口获取完整头像URL
        const userRes = await request({
          url: `${BASE_URL.AUTH}/auth/user/current`,
          method: 'GET'
        });

        if (userRes.code === 200 || userRes.code === "200") {
          saveUserInfo(userRes.data);

          wx.hideLoading();
          wx.showToast({ title: '登录成功', icon: 'success' });
          this.loadUserInfo();
        } else {
          throw new Error(userRes.msg || '获取用户信息失败');
        }
      } else {
        throw new Error(res.msg || '登录失败');
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '登录失败', icon: 'none' });
    }
  },

  // 退出登录
  logout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          // 调用后端登出接口
          request({
            url: `${BASE_URL.AUTH}/auth/logout`,
            method: 'POST',
            header: {
              'Authorization': 'Bearer ' + getToken()
            }
          }).finally(() => {
            clearAuth();
            this.setData({
              isLogin: false,
              isAdmin: false,
              userInfo: null
            });
            wx.showToast({ title: '已退出登录', icon: 'success' });
          });
        }
      }
    });
  },

  // 跳转我的订单
  goOrders() {
    if (!this.data.isLogin) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    wx.switchTab({ url: '/pages/order/order' });
  },

  // 跳转我的收藏
  goFavorites() {
    if (!this.data.isLogin) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: '/pages/favorite/favorite' });
  },

  goVerifyCenter() {
    if (!this.data.isLogin) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    if (!this.data.isAdmin) {
      wx.showToast({ title: '仅管理员可使用', icon: 'none' });
      return;
    }

    wx.navigateTo({ url: '/pages/admin/verify-ticket' });
  }
});
