// pages/order/order-detail.js
const { request, BASE_URL } = require('../../utils/request');

Page({
  data: {
    orderId: null,
    orderDetail: null,
    loading: false,
    qrCodeUrl: '',
    showQrCode: false,
    verifyCode: '',
    statusText: '',
    statusColor: '',
    displayAmount: '',
    showTime: '',
    createTime: '',
    payTime: '',
    cinemaName: '',
    countdown: '',
    timer: null
  },

  onLoad(options) {
    if (options.orderId) {
      this.setData({ orderId: parseInt(options.orderId) });
      this.loadOrderDetail(parseInt(options.orderId));
    }
  },

  onUnload() {
    // 页面卸载时清除定时器
    if (this.data.timer) {
      clearInterval(this.data.timer);
    }
  },

  onHide() {
    // 页面隐藏时清除定时器
    if (this.data.timer) {
      clearInterval(this.data.timer);
    }
  },

  async loadOrderDetail(orderId) {
    this.setData({ loading: true });

    try {
      const res = await request({
        url: `${BASE_URL.ORDER}/order/getOrderDetail/${orderId}`,
        method: 'GET'
      });

      this.setData({ loading: false });

      if (res.code === 200 || res.code === "200") {
        const detail = res.data || {};
        
        // 处理 seatNames
        let seatNames = detail.seatNames;
        if (typeof seatNames === 'string') {
          try {
            seatNames = JSON.parse(seatNames);
          } catch (e) {
            seatNames = [detail.seatNames];
          }
        }
        if (Array.isArray(seatNames)) {
          seatNames = seatNames.join('、');
        }

        // 格式化时间
        const showTime = detail.showTime ? detail.showTime.replace('T', ' ') : '';
        const createTime = detail.createTime ? detail.createTime.replace('T', ' ') : '';
        const payTime = detail.payTime ? detail.payTime.replace('T', ' ') : '';

        this.setData({
          orderDetail: detail,
          statusText: this.getStatusText(detail.status),
          statusColor: this.getStatusColor(detail.status),
          displayAmount: (detail.totalAmount / 100).toFixed(2),
          showTime,
          createTime,
          payTime,
          cinemaName: detail.remark || '',
          qrCodeUrl: detail.qrCodeUrl || '',
          verifyCode: detail.verifyCode || '',
          seatNames: seatNames,
          showQrCode: detail.status === 'PAID' && detail.qrCodeUrl
        });

        // 如果是待支付状态，启动倒计时
        if (detail.status === 'PENDING' && detail.expireTime) {
          this.startCountdown(detail.expireTime);
        }
      } else {
        wx.showToast({ title: res.msg || '加载失败', icon: 'none', duration: 2000 });
        setTimeout(() => {
          wx.navigateBack();
        }, 1500);
      }
    } catch (err) {
      this.setData({ loading: false });
      wx.showToast({ title: '网络异常', icon: 'none', duration: 2000 });
      console.error('加载订单详情失败', err);
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }
  },

  previewQrCode() {
    if (this.data.qrCodeUrl) {
      wx.previewImage({
        urls: [this.data.qrCodeUrl],
        current: this.data.qrCodeUrl
      });
    }
  },

  copyVerifyCode() {
    if (this.data.verifyCode) {
      wx.setClipboardData({
        data: this.data.verifyCode,
        success: () => {
          wx.showToast({ title: '验证码已复制', icon: 'success', duration: 1500 });
        }
      });
    }
  },

  goBack() {
    wx.navigateBack();
  },

  // 支付订单
  payOrder() {
    const { orderId, displayAmount } = this.data;

    if (!orderId) {
      wx.showToast({ title: '订单ID不存在', icon: 'none' });
      return;
    }

    wx.navigateTo({
      url: `/pages/order/payment?orderId=${orderId}&amount=${displayAmount}`
    });
  },

  // 取消订单
  async cancelOrder() {
    const { orderId } = this.data;

    if (!orderId) {
      wx.showToast({ title: '订单ID不存在', icon: 'none' });
      return;
    }

    console.log('取消订单，orderId:', orderId, '类型:', typeof orderId);

    wx.showModal({
      title: '确认取消',
      content: '确定要取消这个订单吗？',
      success: async (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '取消中...', mask: true });

          try {
            const res = await request({
              url: `${BASE_URL.ORDER}/order/cancel?orderId=${orderId}`,
              method: 'POST'
            });

            wx.hideLoading();

            console.log('取消订单响应:', res);

            if (res.code === 200 || res.code === "200") {
              wx.showToast({ title: '订单已取消', icon: 'success', duration: 1500 });
              // 延迟返回，给用户时间看提示
              setTimeout(() => {
                wx.navigateBack();
              }, 1500);
            } else {
              wx.showToast({ title: res.msg || '取消失败', icon: 'none' });
            }
          } catch (err) {
            wx.hideLoading();
            wx.showToast({ title: '网络异常', icon: 'none' });
            console.error('取消订单失败', err);
          }
        }
      }
    });
  },

  // 申请退款
  async refundOrder() {
    const { orderId, displayAmount } = this.data;

    if (!orderId) {
      wx.showToast({ title: '订单ID不存在', icon: 'none' });
      return;
    }

    wx.showModal({
      title: '确认退款',
      content: `订单金额 ¥${displayAmount}，确定要申请退款吗？退款将在1-3个工作日内原路返回。`,
      confirmText: '确认退款',
      confirmColor: '#ff4d4f',
      success: async (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '退款中...', mask: true });

          try {
            const refundRes = await request({
              url: `${BASE_URL.ORDER}/order/refund?orderId=${orderId}`,
              method: 'POST'
            });

            wx.hideLoading();

            console.log('退款响应:', refundRes);

            if (refundRes.code === 200 || refundRes.code === "200") {
              wx.showToast({ title: '退款成功', icon: 'success', duration: 1500 });
              // 刷新订单详情
              setTimeout(() => {
                this.loadOrderDetail(orderId);
              }, 1500);
            } else {
              wx.showToast({ title: refundRes.msg || '退款失败', icon: 'none', duration: 2000 });
            }
          } catch (err) {
            wx.hideLoading();
            wx.showToast({ title: '网络异常', icon: 'none', duration: 2000 });
            console.error('退款失败', err);
          }
        }
      }
    });
  },

  getStatusText(status) {
    const statusMap = {
      'PENDING': '待支付',
      'PAID': '已支付',
      'USED': '已使用',
      'CANCELED': '已取消',
      'EXPIRED': '已过期',
      'REFUND': '已退款'
    };
    return statusMap[status] || status;
  },

  getStatusColor(status) {
    const colorMap = {
      'PENDING': '#ff4d4f',
      'PAID': '#52c41a',
      'USED': '#52c41a',
      'CANCELED': '#999',
      'EXPIRED': '#999',
      'REFUND': '#ff4d4f'
    };
    return colorMap[status] || '#666';
  },

  // 开始倒计时
  startCountdown(expireTime) {
    // 清除之前的定时器
    if (this.data.timer) {
      clearInterval(this.data.timer);
    }

    // 解析过期时间
    const expire = new Date(expireTime).getTime();
    const now = Date.now();

    // 如果已经过期
    if (expire <= now) {
      this.setData({ countdown: '00:00' });
      return;
    }

    // 计算倒计时
    const updateCountdown = () => {
      const currentTime = Date.now();
      const diff = expire - currentTime;

      if (diff <= 0) {
        clearInterval(this.data.timer);
        this.setData({ countdown: '00:00', timer: null });
        // 刷新订单详情
        this.loadOrderDetail(this.data.orderId);
        return;
      }

      const minutes = Math.floor(diff / 60000);
      const seconds = Math.floor((diff % 60000) / 1000);
      this.setData({
        countdown: `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
      });
    };

    // 立即更新一次
    updateCountdown();

    // 启动定时器
    const timer = setInterval(updateCountdown, 1000);
    this.setData({ timer });
  }
});
