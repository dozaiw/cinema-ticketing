// pages/order/order.js
const { request, BASE_URL } = require('../../utils/request');

Page({
  data: {
    orders: [],
    loading: false
  },

  onLoad(options) {
    // 从参数判断是否是查看订单详情
    if (options.orderId) {
      this.navigateToDetail(options.orderId);
    }
  },

  onShow() {
    this.loadOrders();
  },

  async loadOrders() {
    this.setData({ loading: true });

    try {
      const res = await request({
        url: `${BASE_URL.ORDER}/order/queryAllOrder`,
        method: 'GET'
      });

      this.setData({ loading: false });

      if (res.code === 200 || res.code === "200") {
        const orders = (res.data || []).map(order => {
          // 处理 seatNames
          let seatNames = order.seatNames;
          if (typeof seatNames === 'string') {
            try {
              seatNames = JSON.parse(seatNames);
            } catch (e) {
              seatNames = order.seatNames;
            }
          }
          // 如果是数组，用顿号连接
          if (Array.isArray(seatNames)) {
            seatNames = seatNames.join('、');
          }

          return {
            ...order,
            orderId: parseInt(order.orderId || order.id || order.orderNo?.split('_')?.pop()), // 确保orderId是数字
            seatNames: seatNames,
            statusText: this.getStatusText(order.status),
            statusColor: this.getStatusColor(order.status),
            displayAmount: ((order.amount || order.totalAmount || 0) / 100).toFixed(2)
          };
        }).sort((a, b) => {
          // 按创建时间降序排序，最后创建的在第一个
          const timeA = new Date(a.createTime || 0).getTime();
          const timeB = new Date(b.createTime || 0).getTime();
          return timeB - timeA;
        });
        console.log('订单列表:', orders);
        this.setData({ orders });
      } else {
        wx.showToast({ title: res.msg || '加载失败', icon: 'none', duration: 2000 });
      }
    } catch (err) {
      this.setData({ loading: false });
      wx.showToast({ title: '网络异常', icon: 'none', duration: 2000 });
      console.error('加载订单失败', err);
    }
  },

  navigateToDetail(e) {
    const orderId = e.currentTarget.dataset.orderId;
    console.log('点击订单，orderId:', orderId);
    if (orderId) {
      wx.navigateTo({
        url: `/pages/order/order-detail?orderId=${orderId}`,
        fail: (err) => {
          console.error('跳转失败:', err);
          wx.showToast({ title: '跳转失败', icon: 'none' });
        }
      });
    } else {
      console.error('orderId为空');
      wx.showToast({ title: '订单ID不存在', icon: 'none' });
    }
  },

  // 阻止冒泡，点击按钮时不跳转详情
  preventDetail() {
    // 阻止事件冒泡
  },

  // 支付订单
  payOrder(e) {
    const orderId = e.currentTarget.dataset.orderId;
    const amount = e.currentTarget.dataset.amount;

    if (!orderId) {
      wx.showToast({ title: '订单ID不存在', icon: 'none' });
      return;
    }

    wx.navigateTo({
      url: `/pages/order/payment?orderId=${orderId}&amount=${amount}`
    });
  },

  // 取消订单
  async cancelOrder(e) {
    const orderId = e.currentTarget.dataset.orderId;

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
              wx.showToast({ title: '订单已取消', icon: 'success' });
              this.loadOrders();
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
  async refundOrder(e) {
    const orderId = e.currentTarget.dataset.orderId;

    if (!orderId) {
      wx.showToast({ title: '订单ID不存在', icon: 'none' });
      return;
    }

    console.log('申请退款，orderId:', orderId, '类型:', typeof orderId);

    wx.showModal({
      title: '确认退款',
      content: '确定要申请退款吗？退款后将取消订单。',
      success: async (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '退款处理中...', mask: true });

          try {
            const res = await request({
              url: `${BASE_URL.ORDER}/order/refund?orderId=${orderId}`,
              method: 'POST'
            });

            wx.hideLoading();

            console.log('申请退款响应:', res);

            if (res.code === 200 || res.code === "200") {
              wx.showToast({ title: '退款申请成功', icon: 'success' });
              this.loadOrders();
            } else {
              wx.showToast({ title: res.msg || '退款失败', icon: 'none' });
            }
          } catch (err) {
            wx.hideLoading();
            wx.showToast({ title: '网络异常', icon: 'none' });
            console.error('申请退款失败', err);
          }
        }
      }
    });
  },

  goHome() {
    wx.switchTab({
      url: '/pages/index/index'
    });
  },

  onPullDownRefresh() {
    this.loadOrders().then(() => {
      wx.stopPullDownRefresh();
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
  }
});
