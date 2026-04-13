const { request, BASE_URL } = require('../../utils/request');

Page({
  data: {
    movieId: null,
    movie: {},
    dateList: [],
    currentIndex: 0,
    selectedDate: '',
    cinemaList: []  // ✅ 包含影院 + 场次信息
  },

  onLoad(options) {
    const movieId = options.movieId;
    this.setData({ movieId });
    this.initDateList();
    this.loadMovieDetail(movieId);
    this.loadSchedules(movieId);  // ✅ 一次请求获取所有影院 + 场次
  },

  // 初始化日期列表（今天往后 7 天）
  initDateList() {
    const dateList = [];
    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    
    for (let i = 0; i < 7; i++) {
      const date = new Date();
      date.setDate(date.getDate() + i);
      
      const month = date.getMonth() + 1;
      const day = date.getDate();
      const week = i === 0 ? '今天' : weekDays[date.getDay()];
      
      dateList.push({
        date: `${month}-${day}`,
        day: `${month}月${day}日`,
        week: week,
        fullDate: `${date.getFullYear()}-${this.padZero(month)}-${this.padZero(day)}`
      });
    }
    
    this.setData({
      dateList,
      selectedDate: dateList[0].fullDate
    });
  },

  padZero(num) {
    return String(num).padStart(2, '0');
  },

  // 加载电影详情
  async loadMovieDetail(movieId) {
    try {
      const res = await request({
        url: `${BASE_URL.MOVIE}/movie/public/detail/${movieId}`,
        method: 'GET'
      });

      if (res.code === 200 || res.code === "200") {
        this.setData({ movie: res.data });
      }
    } catch (err) {
      console.error('加载电影详情失败', err);
    }
  },

  // ✅ 加载排片（一次请求获取所有影院 + 场次）
  async loadSchedules(movieId) {
    wx.showLoading({ title: '加载中...' });

    try {
      // 调用接口：查询某电影在某天的排片影院列表
      const res = await request({
        url: `${BASE_URL.SCHEDULE}/schedule/public/query/movie/${movieId}/date/${this.data.selectedDate}`,
        method: 'GET'
      });

      wx.hideLoading();

      if (res.code === 200 || res.code === "200") {
        // 后端返回的数据结构：
        // [
        //   {
        //     id: 1,
        //     name: "万达影城",
        //     address: "xxx",
        //     minPrice: 35,
        //     schedules: [  // ✅ 场次数据已经包含在内
        //       { id: 1, startTime: "18:20", hallName: "1 号厅", price: 35 },
        //       { id: 2, startTime: "19:00", hallName: "2 号厅", price: 40 }
        //     ]
        //   }
        // ]
        
        const cinemaList = (res.data || []).map(cinema => ({
          ...cinema,
          tags: ['3D 眼镜免费']
        }));

        this.setData({ cinemaList });

        if (cinemaList.length === 0) {
          wx.showToast({ title: '该日期暂无排片', icon: 'none' });
        }
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '加载失败', icon: 'none' });
      console.error(err);
    }
  },

  // 选择日期 → 重新加载该日期的排片
  selectDate(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({
      currentIndex: index,
      selectedDate: this.data.dateList[index].fullDate
    });
    // ✅ 切换日期后重新请求数据
    this.loadSchedules(this.data.movieId);
  },

  // 格式化时间（只取时分）
  formatTime(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');
    return `${hour}:${minute}`;
  },

  selectSchedule(e) {
    const schedule = e.currentTarget.dataset.schedule;
    
    if (schedule.status === 0) {
      wx.showToast({ title: '该场次暂未开售', icon: 'none' });
      return;
    }
    if (schedule.status === 2) {
      wx.showToast({ title: '该场次已结束', icon: 'none' });
      return;
    }
  
    // ✅ 调试：打印要传递的参数
    const params = {
      scheduleId: schedule.id,
      movieId: this.data.movieId,
      movieName: this.data.movie.title || '',
      hallName: schedule.hallName || '',
      startTime: schedule.startTime || '',
      price: schedule.price || 0
    };
    console.log('🚀 传递到选座页的参数:', params);
  
    // ✅ 构建 URL 并打印
    const url = `/pages/seat/seat?` +
      `scheduleId=${params.scheduleId}` +
      `&movieId=${params.movieId}` +
      `&movieName=${encodeURIComponent(params.movieName)}` +
      `&hallName=${encodeURIComponent(params.hallName)}` +
      `&startTime=${encodeURIComponent(params.startTime)}` +
      `&price=${params.price}`;
      
    console.log('🔗 完整跳转 URL:', url);
  
    wx.navigateTo({ url });
  },

  // 点击影院（可选功能：显示影院详情）
  goCinemaDetail(e) {
    const cinema = e.currentTarget.dataset.cinema;
    const { movieId, selectedDate } = this.data;

    if (!cinema || !cinema.id) {
      console.error('影院数据缺失');
      return;
    }

    wx.navigateTo({
      // 传递三个关键参数：影院 ID、电影 ID、选定日期
      url: `/pages/cinema-detail/cinema-detail?cinemaId=${cinema.id}&movieId=${movieId}&date=${selectedDate}`
    });
  },
});