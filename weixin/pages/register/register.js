const { BASE_URL } = require('../../utils/request');

Page({
  data: {
    phone: '',
    password: '',
    confirmPassword: '',
    nickname: '',
    avatarUrl: ''  // 空表示没选头像
  },

  onLoad() {
    const token = wx.getStorageSync('token');
    if (token) {
      wx.reLaunch({ url: '/pages/index/index' });
    }
  },

  onPhoneInput(e) {
    this.setData({ phone: e.detail.value });
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value });
  },

  onConfirmPasswordInput(e) {
    this.setData({ confirmPassword: e.detail.value });
  },

  onNicknameInput(e) {
    this.setData({ nickname: e.detail.value });
  },

  // 选择头像
  chooseAvatar() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      success: (res) => {
        this.setData({ avatarUrl: res.tempFiles[0].tempFilePath });
      }
    });
  },

  // 清除头像
  clearAvatar() {
    this.setData({ avatarUrl: '' });
  },

  // 注册（统一用一个方法）
  async onRegister() {
    // 表单验证
    if (!this.data.phone || !/^1[3-9]\d{9}$/.test(this.data.phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' });
      return;
    }
    if (!this.data.password || this.data.password.length < 6) {
      wx.showToast({ title: '密码至少 6 位', icon: 'none' });
      return;
    }
    if (this.data.password !== this.data.confirmPassword) {
      wx.showToast({ title: '两次密码不一致', icon: 'none' });
      return;
    }
    if (!this.data.nickname) {
      wx.showToast({ title: '请输入昵称', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '注册中...', mask: true });

    const userData = {
      phone: this.data.phone,
      password: this.data.password,
      nickname: this.data.nickname,
      role: 1,
      status: 1
    };

    // 处理上传响应的公共函数
    const handleUploadResponse = (res, tempFilePath) => {
      wx.hideLoading();

      // 清理临时文件
      if (tempFilePath) {
        const fs = wx.getFileSystemManager();
        try {
          fs.unlinkSync(tempFilePath);
        } catch (e) {
          console.warn('清理临时文件失败', e);
        }
      }

      // 安全解析响应数据
      let data;
      try {
        data = JSON.parse(res.data);
      } catch (e) {
        wx.showToast({ title: '服务器响应错误', icon: 'none' });
        console.error('JSON解析失败', e, res.data);
        return;
      }

      // 处理业务逻辑
      if (data.code === 200 || data.code === "200") {
        wx.showToast({ title: '注册成功', icon: 'success' });
        setTimeout(() => {
          wx.redirectTo({ url: '/pages/login/login' });
        }, 1500);
      } else {
        wx.showToast({ title: data.msg || '注册失败', icon: 'none' });
      }
    };

    // 处理上传失败的公共函数
    const handleUploadError = (err, tempFilePath) => {
      wx.hideLoading();

      // 清理临时文件
      if (tempFilePath) {
        const fs = wx.getFileSystemManager();
        try {
          fs.unlinkSync(tempFilePath);
        } catch (e) {
          console.warn('清理临时文件失败', e);
        }
      }

      wx.showToast({ title: '网络错误，请重试', icon: 'none' });
      console.error('上传失败', err);
    };

    // 后端使用 @RequestPart，必须使用 wx.uploadFile 发送 multipart/form-data
    if (this.data.avatarUrl) {
      // 有头像：正常上传文件
      wx.uploadFile({
        url: `${BASE_URL.AUTH}/auth/regist`,
        filePath: this.data.avatarUrl,
        name: 'avatarFile',
        formData: {
          user: JSON.stringify(userData)
        },
        success: (res) => handleUploadResponse(res, null),
        fail: (err) => handleUploadError(err, null)
      });
    } else {
      // 没有头像：创建临时文件进行上传
      const fs = wx.getFileSystemManager();
      const tempFilePath = `${wx.env.USER_DATA_PATH}/empty_${Date.now()}.txt`;

      try {
        // 创建临时空文件
        fs.writeFileSync(tempFilePath, '');

        wx.uploadFile({
          url: `${BASE_URL.AUTH}/auth/regist`,
          filePath: tempFilePath,
          name: 'avatarFile',
          formData: {
            user: JSON.stringify(userData)
          },
          success: (res) => handleUploadResponse(res, tempFilePath),
          fail: (err) => handleUploadError(err, tempFilePath)
        });
      } catch (e) {
        wx.hideLoading();
        wx.showToast({ title: '注册失败，请重试', icon: 'none' });
        console.error('创建临时文件失败', e);
      }
    }
  },

  goToLogin() {
    wx.redirectTo({ url: '/pages/login/login' });
  }
});