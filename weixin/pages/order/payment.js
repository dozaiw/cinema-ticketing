// pages/order/payment.js
const { request, BASE_URL } = require('../../utils/request');

Page({
  data: {
    orderId: null,
    amount: '0.00',
    password: '',
    showKeyboard: true,
    dots: [false, false, false, false, false, false],
    keyboard: [
      ['1', '2', '3'],
      ['4', '5', '6'],
      ['7', '8', '9'],
      ['', '0', '']
    ],
    // 标记是否是从订单确认页来的
    fromOrderConfirm: false,
    // 用户余额
    balance: 0.00
  },

  onLoad(options) {
    if (options.orderId) {
      this.setData({
        orderId: options.orderId,
        amount: options.amount || '0.00',
        fromOrderConfirm: options.fromOrderConfirm === 'true'
      });
      // 查询账户余额
      this.loadBalance();
    }
  },

  // 查询账户余额
  async loadBalance() {
    try {
      const res = await request({
        url: `${BASE_URL.AUTH}/sandBoxAccount/getSandboxAccount`,
        method: 'GET'
      });
      if (res.code === 200 || res.code === "200") {
        this.setData({
          balance: res.data || 0.00
        });
      }
    } catch (err) {
      console.error('查询余额失败', err);
    }
  },

  // 点击密码输入框
  onDotTap() {
    this.setData({ showKeyboard: true });
  },

  // 点击键盘数字
  onKeyTap(e) {
    const key = e.currentTarget.dataset.key;
    if (!key) return;

    let password = this.data.password;
    if (password.length < 6) {
      password += key;
      this.setData({
        password: password,
        dots: this.data.dots.map((_, index) => index < password.length)
      });

      // 密码输满6位自动提交
      if (password.length === 6) {
        this.submitPayment(password);
      }
    }
  },

  // 点击删除键
  onDeleteTap() {
    let password = this.data.password;
    if (password.length > 0) {
      password = password.slice(0, -1);
      this.setData({
        password: password,
        dots: this.data.dots.map((_, index) => index < password.length)
      });
    }
  },

  // 提交支付
  async submitPayment(password) {
    const { orderId, amount } = this.data;

    wx.showLoading({ title: '支付中...', mask: true });

    try {
      // 使用 URL 参数发送（对应后端 @RequestParam）
      const res = await request({
        url: `${BASE_URL.ORDER}/order/pay`,
        method: 'POST',
        data: {
          orderId: orderId,
          passWord: password
        },
        header: {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      });

      wx.hideLoading();

      if (res.code === 200 || res.code === "200") {
        // 支付成功
        wx.showToast({ title: '支付成功', icon: 'success', duration: 1500 });

        // 如果是从订单确认页来的，返回并刷新
        if (this.data.fromOrderConfirm) {
          setTimeout(() => {
            wx.navigateBack();
          }, 1500);
        } else {
          // 否则显示验证码并返回订单列表
          wx.showModal({
            title: '支付成功',
            content: `验证码：${res.data.verifyCode || ''}`,
            showCancel: false,
            success: () => {
              wx.navigateBack({
                delta: 2, // 返回两层（支付页 -> 订单详情页 -> 订单列表页）
                fail: () => {
                  wx.switchTab({ url: '/pages/order/order' });
                }
              });
            }
          });
        }
      } else {
        // 支付失败，显示后端返回的错误信息
        this.setData({
          password: '',
          dots: [false, false, false, false, false, false]
        });
        wx.showToast({ title: res.msg || '支付失败', icon: 'none' });
      }
    } catch (err) {
      wx.hideLoading();
      // 捕获错误时，如果错误对象中有msg信息，显示msg；否则显示网络异常
      const errorMsg = err.msg || '网络异常';
      this.setData({
        password: '',
        dots: [false, false, false, false, false, false]
      });
      wx.showToast({ title: errorMsg, icon: 'none' });
      console.error('支付失败', err);
    }
  },

  // 取消支付
  onCancelTap() {
    wx.showModal({
      title: '取消支付',
      content: '确定要取消支付吗？',
      success: (res) => {
        if (res.confirm) {
          wx.navigateBack();
        }
      }
    });
  },

  // 忘记密码
  onForgetPassword() {
    wx.showToast({ title: '请联系客服重置密码', icon: 'none' });
  }
});
