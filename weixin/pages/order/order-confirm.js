// pages/order/order-confirm.js
const { request, BASE_URL } = require('../../utils/request');
const { getUserInfo } = require('../../utils/auth');

Page({
  data: {
    scheduleId: null,
    seatIds: [],
    seatNames: [],
    movieName: '',
    hallName: '',
    startTime: '',
    price: 0,
    userPhone: '',

    // 订单信息
    orderId: null,
    orderNo: '',
    showTime: '',
    amount: 0,
    status: '',
    statusText: '',
    expireTime: '',
    verifyCode: '',
    qrCodeUrl: '',
    qrCodeContent: '',
    userPhone: '',

    // 倒计时
    countdown: '',
    timer: null,

    // 支付成功后的票务信息
    paid: false,
    ticketInfo: null
  },

  onLoad(options) {
    const scheduleId = parseInt(options.scheduleId);
    const seatIds = JSON.parse(decodeURIComponent(options.seatIds));
    const movieName = decodeURIComponent(options.movieName);
    const hallName = decodeURIComponent(options.hallName);
    const startTime = decodeURIComponent(options.startTime);
    const price = parseFloat(options.price);

    // 获取用户信息
    const user = getUserInfo();
    const userPhone = user?.phone || '';

    this.setData({
      scheduleId,
      seatIds,
      movieName,
      hallName,
      startTime,
      price,
      userPhone
    });

    // 获取座位名称
    this.loadSeatNames();

    // 创建订单
    this.createOrder();
  },

  onShow() {
    // 页面显示时刷新订单状态（支付成功后返回）
    if (this.data.orderId) {
      this.loadOrderStatus();
    }
  },

  async loadOrderStatus() {
    try {
      const { orderId } = this.data;
      const res = await request({
        url: `${BASE_URL.ORDER}/order/getOrderDetail/${orderId}`,
        method: 'GET'
      });

      if (res.code === 200 || res.code === "200") {
        const detail = res.data;

        // 如果订单已支付，更新票务信息
        if (detail.status === 'PAID' || detail.status === 'USED') {
          // 解析 seatInfo
          let seatInfo = detail.seatNames;
          if (typeof seatInfo === 'string') {
            try {
              seatInfo = JSON.parse(seatInfo);
            } catch (e) {
              seatInfo = [detail.seatNames];
            }
          }
          if (Array.isArray(seatInfo)) {
            seatInfo = seatInfo.join('、');
          }

          this.setData({
            paid: true,
            ticketInfo: {
              movieName: detail.movieName,
              hallName: detail.hallName,
              showTime: detail.showTime ? detail.showTime.replace('T', ' ') : '',
              seatInfo: seatInfo,
              payTime: detail.payTime ? detail.payTime.replace('T', ' ') : ''
            },
            cinemaName: detail.remark || '',
            verifyCode: detail.verifyCode,
            qrCodeUrl: detail.qrCodeUrl
          });

          // 清除倒计时
          if (this.data.timer) {
            clearInterval(this.data.timer);
            this.setData({ timer: null });
          }
        }
      }
    } catch (err) {
      console.error('刷新订单状态失败', err);
    }
  },

  async loadSeatNames() {
    try {
      const { scheduleId } = this.data;
      const res = await request({
        url: `${BASE_URL.SCHEDULE}/seatSchedule/public/query/seatCondition/${scheduleId}`,
        method: 'GET'
      });

      if (res.code === 200 || res.code === "200") {
        const seats = res.data || [];
        const seatMap = {};
        seats.forEach(seat => {
          seatMap[seat.seatId] = seat;
        });

        const seatNames = this.data.seatIds.map(seatId => {
          const seat = seatMap[seatId];
          return seat ? `${seat.rowNum}排${seat.colNum}座` : '';
        }).filter(Boolean);

        this.setData({ seatNames });
      }
    } catch (err) {
      console.error('加载座位信息失败', err);
    }
  },

  async createOrder() {
    wx.showLoading({ title: '创建订单...', mask: true });

    try {
      const { scheduleId, seatIds, hallName, movieName, startTime, userPhone, price } = this.data;
      const res = await request({
        url: `${BASE_URL.ORDER}/order/create`,
        method: 'POST',
        data: {
          scheduleId,
          seatIds,
          hallName,
          movieName,
          showTime: startTime,
          userPhone,
          price
        }
      });

      wx.hideLoading();

      if (res.code === 200 || res.code === "200") {
        const order = res.data;
        this.setData({
          orderId: order.orderId,
          orderNo: order.orderNo,
          showTime: order.showTime,
          hallName: order.hallName || this.data.hallName,
          cinemaName: order.cinemaName || order.remark || '',
          amount: (order.amount / 100).toFixed(2),
          status: order.status,
          statusText: order.statusText,
          expireTime: order.expireTime,
          verifyCode: order.verifyCode,
          qrCodeUrl: order.qrCodeUrl,
          qrCodeContent: order.qrCodeContent,
          userPhone: order.userPhone
        });

        // 开始倒计时
        this.startCountdown();
      } else {
        wx.showToast({ title: res.msg || '创建订单失败', icon: 'none', duration: 2000 });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '网络异常', icon: 'none', duration: 2000 });
      console.error('创建订单失败', err);
    }
  },

  // 跳转到支付页面
  goToPayment() {
    if (this.data.paid) return;

    const { orderId, amount } = this.data;

    if (!orderId) {
      wx.showToast({ title: '订单ID不存在', icon: 'none' });
      return;
    }

    wx.navigateTo({
      url: `/pages/order/payment?orderId=${orderId}&amount=${amount}&fromOrderConfirm=true`
    });
  },

  startCountdown() {
    const expireTime = new Date(this.data.expireTime).getTime();
    
    if (this.data.timer) {
      clearInterval(this.data.timer);
    }

    const timer = setInterval(() => {
      const now = Date.now();
      const diff = expireTime - now;

      if (diff <= 0) {
        clearInterval(timer);
        this.setData({ countdown: '00:00', timer: null });
        return;
      }

      const minutes = Math.floor(diff / 60000);
      const seconds = Math.floor((diff % 60000) / 1000);
      this.setData({
        countdown: `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
      });
    }, 1000);

    this.setData({ timer });
  },

  async handlePay() {
    if (this.data.paid) return;

    wx.showLoading({ title: '支付中...', mask: true });

    try {
      const { orderId } = this.data;
      const res = await request({
        url: `${BASE_URL.ORDER}/order/pay?orderId=${orderId}`,
        method: 'POST'
      });

      wx.hideLoading();

      if (res.code === 200 || res.code === "200") {
        const data = res.data;
        // 解析 seatInfo（可能是 JSON 字符串）
        let seatInfo = data.seatInfo;
        if (typeof seatInfo === 'string') {
          try {
            seatInfo = JSON.parse(seatInfo);
          } catch (e) {
            seatInfo = data.seatInfo;
          }
        }
        // 如果是数组，转换为用顿号分隔的字符串
        if (Array.isArray(seatInfo)) {
          seatInfo = seatInfo.join('、');
        }

        this.setData({
          paid: true,
          ticketInfo: {
            ...data,
            seatInfo: seatInfo
          },
          cinemaName: data.remark || '',
          verifyCode: data.verifyCode,
          qrCodeUrl: data.qrCodeUrl,
          qrCodeContent: data.qrCodeContent
        });

        // 清除倒计时
        if (this.data.timer) {
          clearInterval(this.data.timer);
          this.setData({ timer: null });
        }

        wx.showToast({ title: '支付成功', icon: 'success', duration: 2000 });
      } else {
        wx.showToast({ title: res.msg || '支付失败', icon: 'none', duration: 2000 });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '网络异常', icon: 'none', duration: 2000 });
      console.error('支付失败', err);
    }
  },

  async handleCancel() {
    if (this.data.paid) {
      wx.showToast({ title: '订单已支付，无法取消', icon: 'none', duration: 2000 });
      return;
    }

    wx.showModal({
      title: '确认取消',
      content: '确定要取消这个订单吗？',
      success: async (res) => {
        if (res.confirm) {
          await this.cancelOrder();
        }
      }
    });
  },

  // 退款
  async handleRefund() {
    if (!this.data.paid) {
      wx.showToast({ title: '订单未支付，无法退款', icon: 'none', duration: 2000 });
      return;
    }

    wx.showModal({
      title: '确认退款',
      content: '确定要申请退款吗？',
      success: async (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '退款中...', mask: true });

          try {
            const { orderId } = this.data;
            const refundRes = await request({
              url: `${BASE_URL.ORDER}/order/refund`,
              method: 'POST',
              data: {
                orderId: orderId
              }
            });

            wx.hideLoading();

            if (refundRes.code === 200 || refundRes.code === "200") {
              wx.showToast({ title: '退款成功', icon: 'success', duration: 2000 });

              // 更新订单状态
              this.setData({ paid: false });

              // 延迟返回
              setTimeout(() => {
                wx.navigateBack();
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

  async cancelOrder() {
    wx.showLoading({ title: '取消中...', mask: true });

    try {
      const { orderId } = this.data;
      const res = await request({
        url: `${BASE_URL.ORDER}/order/cancel?orderId=${orderId}`,
        method: 'POST'
      });

      wx.hideLoading();

      if (res.code === 200 || res.code === "200") {
        // 清除倒计时
        if (this.data.timer) {
          clearInterval(this.data.timer);
        }

        wx.showToast({ title: '订单已取消', icon: 'success', duration: 2000 });
        
        // 延迟返回
        setTimeout(() => {
          wx.navigateBack();
        }, 1500);
      } else {
        wx.showToast({ title: res.msg || '取消失败', icon: 'none', duration: 2000 });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '网络异常', icon: 'none', duration: 2000 });
      console.error('取消订单失败', err);
    }
  },

  copyVerifyCode() {
    if (!this.data.verifyCode) return;
    
    wx.setClipboardData({
      data: this.data.verifyCode,
      success: () => {
        wx.showToast({ title: '验证码已复制', icon: 'success', duration: 1500 });
      }
    });
  },

  navigateToOrderList() {
    wx.switchTab({
      url: '/pages/order/order'
    });
  },

  onUnload() {
    if (this.data.timer) {
      clearInterval(this.data.timer);
    }
  }
});
