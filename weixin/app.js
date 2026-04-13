const { request, BASE_URL } = require('./utils/request');

App({
  globalData: {
    userInfo: null,
    token: null
  },

  onLaunch() {
    this.checkLoginStatus();
  },

  // 检查登录状态
  async checkLoginStatus() {
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');

    if (token && userInfo) {
      try {
        // 验证 token 是否有效
        const res = await request({
          url: `${BASE_URL.AUTH}/auth/user/current`,
          method: 'GET'
        });

        if (res.code === 200) {
          // token 有效，保存全局信息
          this.globalData.token = token;
          this.globalData.userInfo = JSON.parse(userInfo);
          console.log('登录态有效');
        } else {
          // token 失效，清除本地存储
          this.clearLoginInfo();
        }
      } catch (err) {
        // 请求失败，清除登录信息
        this.clearLoginInfo();
      }
    }
  },

  // 清除登录信息
  clearLoginInfo() {
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
    this.globalData.token = null;
    this.globalData.userInfo = null;
  },

  // 获取全局用户信息
  getUserInfo() {
    return this.globalData.userInfo;
  },

  // 获取全局 Token
  getToken() {
    return this.globalData.token;
  }
});