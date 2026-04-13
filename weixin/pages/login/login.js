const { request, BASE_URL } = require('../../utils/request');
const { getToken, saveUserInfo } = require('../../utils/auth');

Page({
  data: {
    username: '',
    password: ''
  },

  onLoad() {
    // 检查是否已有 token，有则直接跳转首页
    const token = getToken();
    if (token) {
      wx.reLaunch({ url: '/pages/index/index' });
    }
  },

  onUsernameInput(e) {
    this.setData({ username: e.detail.value });
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value });
  },

  // 登录
  async onLogin() {
    // 1. 表单验证
    if (!this.data.username) {
      wx.showToast({ title: '请输入手机号', icon: 'none' });
      return;
    }
    if (!this.data.password) {
      wx.showToast({ title: '请输入密码', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '登录中...', mask: true });

    try {
      // 2. 调用登录接口
      const res = await request({
        url: `${BASE_URL.AUTH}/auth/login`,
        method: 'POST',
        data: {
          username: this.data.username,
          password: this.data.password
        }
      });

      if (res.code === 200 || res.code === "200") {
        // 3. 保存 Token
        const token = res.data.token || res.data;
        wx.setStorageSync('token', token);

        // 4. 调用获取用户信息接口获取完整头像URL
        const userRes = await request({
          url: `${BASE_URL.AUTH}/auth/user/current`,
          method: 'GET'
        });

        if (userRes.code === 200 || userRes.code === "200") {
          saveUserInfo(userRes.data);

          wx.hideLoading();
          wx.showToast({ title: '登录成功', icon: 'success' });

          // 5. 跳转首页
          setTimeout(() => {
            wx.reLaunch({ url: '/pages/index/index' });
          }, 1000);
        } else {
          throw new Error(userRes.msg || '获取用户信息失败');
        }
      } else {
        throw new Error(res.msg || '登录失败');
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: err.message || '登录失败', icon: 'none' });
    }
  },

  // 跳转注册
  goToRegister() {
    wx.redirectTo({ url: '/pages/register/register' });
  }
});
