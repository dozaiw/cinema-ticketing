const { request, BASE_URL } = require('../../utils/request');

Page({
  data: {
    cinemaId: null,
    movieId: null,
    cinemaName: '',
    cinema: {},
    movie: {},
    dateList: [],
    currentIndex: 0,
    selectedDate: '',
    selectedDateLabel: '今日排片',
    scheduleList: []
  },

  onLoad(options) {
    const initialDate = options.date ? decodeURIComponent(options.date) : '';
    this.setData({
      cinemaId: options.cinemaId,
      movieId: options.movieId,
      cinemaName: options.cinemaName ? decodeURIComponent(options.cinemaName) : '影院'
    });

    const { dateList, selectedDate, currentIndex, selectedDateLabel } = this.initDateList(initialDate);
    this.setData({ dateList, selectedDate, currentIndex, selectedDateLabel });

    this.loadCinemaInfo();
    this.loadMovieInfo();
    this.loadSchedules(selectedDate);
  },

  initDateList(initialDate = '') {
    const dateList = [];
    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

    for (let i = 0; i < 7; i++) {
      const date = new Date();
      date.setDate(date.getDate() + i);

      const month = date.getMonth() + 1;
      const day = date.getDate();
      const week = i === 0 ? '今天' : weekDays[date.getDay()];
      const fullDate = `${date.getFullYear()}-${this.padZero(month)}-${this.padZero(day)}`;

      dateList.push({
        date: `${month}-${day}`,
        day: `${month}月${day}日`,
        week: week,
        fullDate: fullDate
      });
    }

    const matchedIndex = dateList.findIndex(item => item.fullDate === initialDate);
    const currentIndex = matchedIndex >= 0 ? matchedIndex : 0;
    const selectedItem = dateList[currentIndex];

    return {
      dateList,
      currentIndex,
      selectedDate: selectedItem.fullDate,
      selectedDateLabel: this.formatSelectedDateLabel(selectedItem)
    };
  },

  padZero(num) {
    return String(num).padStart(2, '0');
  },

  async loadCinemaInfo() {
    try {
      const res = await request({
        url: `${BASE_URL.SCHEDULE}/cinema/public/detail/${this.data.cinemaId}`,
        method: 'GET'
      });

      if (res.code === 200 || res.code === '200') {
        const cinemaData = res.data.cinema || res.data;

        this.setData({
          cinema: cinemaData,
          cinemaName: cinemaData.name || this.data.cinemaName
        });
      }
    } catch (err) {
      console.error('加载影院信息失败', err);
    }
  },

  async loadMovieInfo() {
    try {
      const res = await request({
        url: `${BASE_URL.MOVIE}/movie/public/detail/${this.data.movieId}`,
        method: 'GET'
      });

      if (res.code === 200 || res.code === '200') {
        const movieData = res.data.movie || res.data;
        this.setData({ movie: movieData });
      }
    } catch (err) {
      console.error('加载电影信息失败', err);
    }
  },

  async loadSchedules(date) {
    const requestDate = date || this.data.selectedDate;
    if (!requestDate) return;

    wx.showLoading({ title: '加载中...' });

    try {
      const res = await request({
        url: `${BASE_URL.SCHEDULE}/schedule/public/movie/${this.data.movieId}/cinema/${this.data.cinemaId}/date/${requestDate}`,
        method: 'GET'
      });

      wx.hideLoading();

      if (res.code === 200 || res.code === '200') {
        this.setData({ scheduleList: res.data || [] });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '加载失败', icon: 'none' });
      console.error(err);
    }
  },

  selectDate(e) {
    const index = e.currentTarget.dataset.index;
    const selectedItem = this.data.dateList[index];
    const newDate = selectedItem.fullDate;

    this.setData({
      currentIndex: index,
      selectedDate: newDate,
      selectedDateLabel: this.formatSelectedDateLabel(selectedItem)
    });

    this.loadSchedules(newDate);
  },

  formatSelectedDateLabel(dateItem) {
    if (!dateItem) return '今日排片';
    return dateItem.week === '今天' ? '今日排片' : `${dateItem.day}排片`;
  },

  formatTime(dateStr) {
    if (!dateStr) return '';
    if (/^\d{2}:\d{2}$/.test(dateStr)) return dateStr;

    const timeMatch = dateStr.match(/(\d{2}):(\d{2}):?\d*/);
    if (timeMatch) {
      return `${timeMatch[1]}:${timeMatch[2]}`;
    }

    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;

    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');
    return `${hour}:${minute}`;
  },

  openLocation() {
    const cinema = this.data.cinema;
    if (!cinema.latitude || !cinema.longitude) {
      wx.showToast({ title: '位置信息不可用', icon: 'none' });
      return;
    }

    wx.openLocation({
      latitude: Number(cinema.latitude),
      longitude: Number(cinema.longitude),
      name: cinema.name || this.data.cinemaName,
      address: cinema.address || '',
      scale: 16
    });
  },

  selectSchedule(e) {
    const schedule = e.currentTarget.dataset.schedule;

    if (schedule.displayStatus === 2 || schedule.status === 0) {
      wx.showToast({ title: '该场次已售罄', icon: 'none' });
      return;
    }

    wx.navigateTo({
      url: `/pages/seat/seat?scheduleId=${schedule.id}&movieId=${this.data.movieId}&cinemaId=${this.data.cinemaId}`
    });
  }
});
