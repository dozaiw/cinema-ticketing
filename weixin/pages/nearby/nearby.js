const { request, BASE_URL } = require('../../utils/request');

Page({
  data: {
    cinemaList: [],
    currentLocation: '',
    latitude: null,
    longitude: null
  },

  onLoad() {
    this.getLocationAndLoadCinemas();
  },

  // 获取位置并加载影院
  async getLocationAndLoadCinemas() {
    wx.showLoading({ title: '定位中...' });

    try {
      // 方案 A：真实获取位置（真机测试用）
      const locationRes = await new Promise((resolve, reject) => {
        wx.getLocation({
          type: 'gcj02',
          success: resolve,
          fail: reject
        });
      });

      this.setData({
        latitude: locationRes.latitude,
        longitude: locationRes.longitude,
        currentLocation: `${locationRes.latitude.toFixed(4)}, ${locationRes.longitude.toFixed(4)}`
      });

      await this.loadNearbyCinemas(locationRes.latitude, locationRes.longitude);
    } catch (err) {
      console.error('获取位置失败', err);
      
      // ✅ 方案 B：定位失败用默认位置（毕设演示用）
      wx.showToast({ title: '定位失败，使用默认位置', icon: 'none' });
      // 默认北京中关村
      this.setData({
        latitude: 39.9042,
        longitude: 116.4074,
        currentLocation: '北京中关村（默认）'
      });
      await this.loadNearbyCinemas(39.9042, 116.4074);
    } finally {
      // ✅ 确保 hideLoading 一定会执行
      wx.hideLoading();
    }
  },

  // 调用后端附近影院接口
  async loadNearbyCinemas(latitude, longitude) {
    try {
      const res = await request({
        url: `${BASE_URL.SCHEDULE}/cinema/nearby?latitude=${latitude}&longitude=${longitude}`,
        method: 'GET'
      });

      if (res.code === 200 || res.code === "200") {
        const cinemaList = res.data || [];
        this.setData({ cinemaList: cinemaList });

        if (cinemaList.length === 0) {
          wx.showToast({ title: '附近暂无影院', icon: 'none' });
        }
      } else {
        wx.showToast({ title: res.msg || '加载失败', icon: 'none' });
      }
    } catch (err) {
      wx.showToast({ title: '网络错误', icon: 'none' });
      console.error(err);
    }
  },

  // 格式化距离显示
  formatDistance(meters) {
    if (meters >= 1000) {
      return (meters / 1000).toFixed(1) + 'km';
    } else {
      return Math.round(meters) + 'm';
    }
  },

  // 刷新位置
  refreshLocation() {
    this.setData({
      cinemaList: [],
      currentLocation: '定位中...'
    });
    this.getLocationAndLoadCinemas();
  },

  // 点击影院跳转
  goCinemaDetail(e) {
    const cinema = e.currentTarget.dataset.cinema || {};
    const latitude = Number(cinema.latitude);
    const longitude = Number(cinema.longitude);

    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      wx.showToast({
        title: '该影院暂未配置地图坐标',
        icon: 'none'
      });
      return;
    }

    wx.openLocation({
      latitude,
      longitude,
      name: cinema.name || '影院',
      address: cinema.address || '',
      scale: 16
    });
  }
});
