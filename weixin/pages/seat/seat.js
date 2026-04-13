// pages/seat/seat.js
const { request, BASE_URL } = require('../../utils/request');

const SEAT_SIZE = 50;
const SEAT_GAP = 12;

Page({
  data: {
    scheduleId: null,
    movieId: null,
    movieName: '',
    movie: {},
    hallName: '',
    startTime: '',
    endTime: '',
    price: 0,
    showStartTime: '',
    showEndTime: '',

    seats: [],
    seatRows: [],
    seatMap: {},

    selectedSeatIds: [],
    selectedSeatInfo: [],
    totalPrice: 0,

    seatSize: SEAT_SIZE,
    seatGap: SEAT_GAP,
    totalSeatWidth: 0,
    _force: 0,

    // 用于调试
    debugData: '',

    // WebSocket 连接
    socketConnected: false
  },

  onLoad(options) {
    const movieName = options.movieName ? decodeURIComponent(options.movieName) : '';
    const hallName = options.hallName ? decodeURIComponent(options.hallName) : '';
    const startTime = options.startTime ? decodeURIComponent(options.startTime) : '';

    this.setData({
      scheduleId: parseInt(options.scheduleId),
      movieId: parseInt(options.movieId),
      movieName: movieName,
      hallName: hallName,
      startTime: startTime,
      price: parseFloat(options.price || 0),
      showStartTime: this.formatShowTime(startTime)
    });

    if (!movieName && this.data.movieId) {
      this.loadMovieDetail(this.data.movieId);
    }

    if (this.data.scheduleId) {
      this.loadScheduleDetail(this.data.scheduleId);
    }

    this.loadSeatData();

    // 连接 WebSocket
    this.connectWebSocket();
  },

  onReady() {
    console.log('页面渲染完成，座位数据:', this.data.seatRows);
  },

  onUnload() {
    // 页面卸载时关闭 WebSocket
    this.closeWebSocket();
  },

  onHide() {
    // 页面隐藏时关闭 WebSocket
    this.closeWebSocket();
  },

  onShow() {
    // 页面显示时重新连接 WebSocket
    this.connectWebSocket();
  },

  // 连接 WebSocket
  connectWebSocket() {
    const { scheduleId } = this.data;
    const token = wx.getStorageSync('token');
    if (!scheduleId || !token) {
      return;
    }

    if (this.socketTask && (this.data.socketConnected || this.socketConnecting) && this.socketScheduleId === scheduleId) {
      return;
    }

    if (this.socketTask) {
      this.closeWebSocket();
    }

    // 格式：ws://{BASE_URL.SCHEDULE}/ws/seat/{scheduleId}?token={token}
    const wsBaseUrl = BASE_URL.SCHEDULE.replace(/^http/, 'ws');
    const wsUrl = `${wsBaseUrl}/ws/seat/${scheduleId}?token=${encodeURIComponent(token)}`;

    console.log('连接 WebSocket:', wsUrl);

    this.socketConnecting = true;
    const socketTask = wx.connectSocket({
      url: wsUrl,
      success: () => {
        console.log('WebSocket 连接发起成功');
      },
      fail: (err) => {
        this.socketConnecting = false;
        console.error('WebSocket 连接发起失败', err);
        // WebSocket 连接失败不影响页面使用
      }
    });
    this.socketTask = socketTask;
    this.socketScheduleId = scheduleId;

    socketTask.onOpen((res) => {
      console.log('✅ WebSocket 连接已打开', res);
      this.socketConnecting = false;
      this.setData({ socketConnected: true });
    });

    socketTask.onMessage((res) => {
      console.log('📨 收到 WebSocket 消息:', res.data);
      console.log('当前 selectedSeatIds:', this.data.selectedSeatIds);
      this.handleSeatUpdate(res.data);
    });

    socketTask.onError((err) => {
      console.error('WebSocket 错误:', err);
      this.socketConnecting = false;
      this.setData({ socketConnected: false });
    });

    socketTask.onClose((res) => {
      console.log('WebSocket 连接已关闭', res);
      this.socketConnecting = false;
      if (this.socketTask === socketTask) {
        this.socketTask = null;
        this.socketScheduleId = null;
      }
      this.setData({ socketConnected: false });
    });
  },

  // 关闭 WebSocket
  closeWebSocket() {
    if (this.socketTask) {
      const socketTask = this.socketTask;
      this.socketTask = null;
      this.socketScheduleId = null;
      this.socketConnecting = false;
      try {
        socketTask.close({
          code: 1000,
          reason: 'page hide'
        });
      } catch (err) {
        console.error('关闭 WebSocket 失败', err);
      }
      console.log('WebSocket 连接已主动关闭');
      this.setData({ socketConnected: false });
    }
  },

  // 处理座位更新消息
  handleSeatUpdate(message) {
    try {
      console.log('收到原始消息:', message);
      const data = JSON.parse(message);
      console.log('解析后的数据:', data);

      const { seatScheduleId, seatStatus, userId, seatId } = data;
      const actualSeatId = Number(seatScheduleId || seatId);
      const currentUserId = this.getCurrentUserId();

      if (!actualSeatId) {
        console.error('消息中缺少座位ID:', data);
        return;
      }

      console.log('座位更新:', data);

      // 更新 seatMap
      const newSeatMap = { ...this.data.seatMap };
      if (newSeatMap[actualSeatId]) {
        newSeatMap[actualSeatId] = {
          ...newSeatMap[actualSeatId],
          seatStatus: seatStatus,
          userId: userId
        };
      }

      // 如果座位被锁定了，且是自己选的，从已选列表中移除
      if (seatStatus === 1 && this.data.selectedSeatIds.includes(actualSeatId)) {
        // 如果是自己锁定的，不用移除
        if (currentUserId && String(userId) !== String(currentUserId)) {
          const newSelectedIds = this.data.selectedSeatIds.filter(id => id !== actualSeatId);
          this.setData({ selectedSeatIds: newSelectedIds });
          this.updateSelectedSeats(newSelectedIds);
          wx.showToast({ title: '座位已被他人选走', icon: 'none', duration: 1500 });
        }
      }

      // 更新 seats
      const newSeats = this.data.seats.map(seat =>
        seat.seatId === actualSeatId
          ? { ...seat, seatStatus: seatStatus, userId: userId }
          : seat
      );

      // 更新 seatRows
      const newSeatRows = this.data.seatRows.map(row => ({
        ...row,
        seats: row.seats.map(s => {
          if (s.seatId === actualSeatId) {
            let bgColor = '#52c41a'; // 可选-绿色
            if (this.data.selectedSeatIds.includes(s.seatId)) {
              bgColor = '#fa8c16'; // 已选-橙色
            } else if (seatStatus === 1) {
              bgColor = '#ff4d4f'; // 已售-红色
            }

            return {
              ...s,
              seatStatus: seatStatus,
              userId: userId,
              seatStyle: `width: 36px; height: 36px; margin: 4px; background-color: ${bgColor}; border-radius: 6px; display: flex; align-items: center; justify-content: center;`
            };
          }
          return s;
        })
      }));

      this.setData({
        seatMap: newSeatMap,
        seats: newSeats,
        seatRows: newSeatRows
      });

    } catch (e) {
      console.error('解析 WebSocket 消息失败:', e);
    }
  },

  async loadMovieDetail(movieId) {
    try {
      const res = await request({
        url: `${BASE_URL.MOVIE}/movie/public/detail/${movieId}`,
        method: 'GET'
      });
      if (res.code === 200 || res.code === "200") {
        const movie = res.data;
        this.setData({
          movie: movie,
          movieName: movie.title || this.data.movieName
        });
        if (movie.title) {
          wx.setNavigationBarTitle({ title: movie.title });
        }
      }
    } catch (err) {
      console.error('加载电影详情失败', err);
    }
  },

  async loadScheduleDetail(scheduleId) {
    try {
      const res = await request({
        url: `${BASE_URL.SCHEDULE}/schedule/public/query/schedule/${scheduleId}`,
        method: 'GET'
      });

      if (res.code === 200 || res.code === "200") {
        const schedule = res.data;
        const endTime = schedule.endTime || schedule.end_time || '';
        const startTime = schedule.startTime || schedule.start_time || '';
        
        const showStartTime = this.formatShowTime(startTime);
        const showEndTime = this.formatShowTime(endTime);
        
        this.setData({
          endTime: endTime,
          showStartTime: showStartTime,
          showEndTime: showEndTime,
          hallName: schedule.hallName || schedule.hall_name || this.data.hallName,
          startTime: startTime || this.data.startTime,
          price: schedule.price || this.data.price
        });
        
        this.setData({ _force: Date.now() });
      }
    } catch (err) {
      console.error('加载排片详情失败', err);
    }
  },

  async loadSeatData() {
    wx.showLoading({ title: '加载座位...', mask: true });
    
    try {
      const { scheduleId } = this.data;
      const res = await request({
        url: `${BASE_URL.SCHEDULE}/seatSchedule/public/query/seatCondition/${scheduleId}`,
        method: 'GET'
      });

      wx.hideLoading();
      
      if (res.code === 200 || res.code === "200") {
        const seats = res.data || [];
        console.log('座位数据:', seats);

        const seatMap = {};
        seats.forEach(seat => {
          seatMap[seat.seatId] = seat;
        });

        const seatRowsMap = {};
        let maxCols = 0;

        seats.forEach(seat => {
          const row = seat.rowNum;
          const col = seat.colNum;

          if (!seatRowsMap[row]) {
            seatRowsMap[row] = [];
          }

          // 计算座位样式
          let bgColor = '#52c41a'; // 可选-绿色
          if (seat.seatStatus === 1) {
            bgColor = '#ff4d4f'; // 已售-红色
          }

          const seatWithStyle = {
            ...seat,
            seatRow: row,
            seatCol: col,
            seatStyle: `width: 36px; height: 36px; margin: 4px; background-color: ${bgColor}; border-radius: 6px; display: flex; align-items: center; justify-content: center;`
          };

          seatRowsMap[row].push(seatWithStyle);

          if (seatRowsMap[row].length > maxCols) {
            maxCols = seatRowsMap[row].length;
          }
        });

        const seatRows = Object.keys(seatRowsMap)
          .map(rowNum => ({
            rowNum: parseInt(rowNum),
            seats: seatRowsMap[rowNum].sort((a, b) => a.seatCol - b.seatCol)
          }))
          .sort((a, b) => a.rowNum - b.rowNum);

        console.log('座位行数据:', seatRows);

        // 使用同步API获取系统信息
        const systemInfo = wx.getSystemInfoSync();
        const screenWidth = systemInfo.windowWidth;
        const totalWidth = Math.min(
          maxCols * (SEAT_SIZE + SEAT_GAP),
          screenWidth - 40
        );

        this.setData({
          seats,
          seatRows,
          seatMap,
          totalSeatWidth: totalWidth,
          selectedSeatIds: [],
          selectedSeatInfo: [],
          totalPrice: 0,
          debugData: `共${seats.length}个座位，${seatRows.length}排`
        });
      } else {
        console.log('座位加载失败:', res);
        wx.showToast({ title: res.msg || '加载失败', icon: 'none', duration: 2000 });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '网络异常', icon: 'none', duration: 2000 });
      console.error('加载座位失败', err);
    }
  },

  async handleSeatTap(e) {
    const seatId = Number(e.currentTarget.dataset.seatId);
    const seat = this.data.seatMap[seatId];

    if (!seat) {
      console.error('座位数据未找到:', seatId);
      return;
    }

    const { seatStatus } = seat;
    const { selectedSeatIds } = this.data;

    const index = selectedSeatIds.indexOf(seatId);

    // 先检查是否是已选座位
    if (index > -1) {
      // 取消选择
      const newSelectedIds = [...selectedSeatIds];
      newSelectedIds.splice(index, 1);

      this.updateSelectedSeats(newSelectedIds);
      this.refreshSeatStatus(seatId, 0, newSelectedIds);
      this.cancelPreselect(seatId);
      return;
    }

    // 🔥 已售不可选（包括被他人锁定）
    if (seatStatus === 1) {
      wx.showToast({ title: '该座位已售出', icon: 'none', duration: 1500 });
      return;
    }

    // 选择座位
    const newSelectedIds = [...selectedSeatIds, seatId];

    this.updateSelectedSeats(newSelectedIds);
    this.refreshSeatStatus(seatId, 2, newSelectedIds);

    const success = await this.preselectSeat(seatId);
    if (!success) {
      const rollbackIds = newSelectedIds.filter(id => id !== seatId);
      this.updateSelectedSeats(rollbackIds);
      this.refreshSeatStatus(seatId, 0, rollbackIds);
    }
  },

  async preselectSeat(seatId) {
    try {
      const { scheduleId } = this.data;
      const res = await request({
        url: `${BASE_URL.SCHEDULE}/seatSchedule/preselectSeat`,
        method: 'POST',
        data: { seatId, scheduleId }
      });

      if (res.code === 200 || res.code === "200") {
        console.log('✅ 预占成功:', seatId);
        return true;
      } else {
        wx.showToast({ title: res.msg || '预占失败', icon: 'none', duration: 1500 });
        return false;
      }
    } catch (err) {
      wx.showToast({ title: '网络异常', icon: 'none', duration: 1500 });
      console.error('❌ 预占异常:', err);
      return false;
    }
  },

  refreshSeatStatus(seatId, newStatus = 0, selectedSeatIds = null) {
    const { seats, seatRows, seatMap } = this.data;
    const targetIds = selectedSeatIds || this.data.selectedSeatIds;
    const currentUserId = this.getCurrentUserId();

    const newSeatMap = { ...seatMap };
    if (newSeatMap[seatId]) {
      newSeatMap[seatId] = { ...newSeatMap[seatId], seatStatus: newStatus };
    }

    const newSeats = seats.map(seat =>
      seat.seatId === seatId
        ? { ...seat, seatStatus: newStatus }
        : seat
    );

    const newSeatRows = seatRows.map(row => ({
      ...row,
      seats: row.seats.map(s => {
        if (s.seatId === seatId) {
          let bgColor = '#52c41a'; // 可选-绿色
          if (targetIds.includes(s.seatId)) {
            bgColor = '#fa8c16'; // 已选-橙色
          } else if (newStatus === 1) {
            bgColor = '#ff4d4f'; // 已售-红色
          }

          return {
            ...s,
            seatStatus: newStatus,
            userId: currentUserId ? String(currentUserId) : null,
            seatStyle: `width: 36px; height: 36px; margin: 4px; background-color: ${bgColor}; border-radius: 6px; display: flex; align-items: center; justify-content: center;`
          };
        }
        return s;
      })
    }));

    this.setData({
      seats: newSeats,
      seatRows: newSeatRows,
      seatMap: newSeatMap,
      selectedSeatIds: targetIds
    });
  },

  updateSelectedSeats(newSelectedIds = null) {
    const targetIds = newSelectedIds || this.data.selectedSeatIds;
    const { seats, seatRows, price } = this.data;

    const selectedSeatInfo = targetIds.map(seatId => {
      const seat = seats.find(s => s.seatId === seatId);
      return seat ? `${seat.rowNum}排${seat.colNum}座` : '';
    }).filter(Boolean);

    // 计算总价
    const total = price * targetIds.length;
    const totalPrice = Number.isInteger(total) ? total : total.toFixed(1);

    // 更新所有座位的样式
    const newSeatRows = seatRows.map(row => ({
      ...row,
      seats: row.seats.map(s => {
        let bgColor = '#52c41a'; // 可选-绿色
        if (targetIds.includes(s.seatId)) {
          bgColor = '#fa8c16'; // 已选-橙色
        } else if (s.seatStatus === 1) {
          bgColor = '#ff4d4f'; // 已售-红色
        }

        return {
          ...s,
          seatStyle: `width: 36px; height: 36px; margin: 4px; background-color: ${bgColor}; border-radius: 6px; display: flex; align-items: center; justify-content: center;`
        };
      })
    }));

    this.setData({
      selectedSeatIds: targetIds,
      selectedSeatInfo,
      seatRows: newSeatRows,
      totalPrice
    });
  },

  removeSeat(e) {
    const index = e.currentTarget.dataset.index;
    const { selectedSeatIds } = this.data;
    
    const seatId = selectedSeatIds[index];
    const newSelectedIds = [...selectedSeatIds];
    newSelectedIds.splice(index, 1);
    
    this.cancelPreselect(seatId);
    this.updateSelectedSeats(newSelectedIds);
    this.refreshSeatStatus(seatId, 0, newSelectedIds);
  },

  async cancelPreselect(seatId) {
    try {
      const { scheduleId } = this.data;
      await request({
        url: `${BASE_URL.SCHEDULE}/seatSchedule/cancelSeat/${scheduleId}/${seatId}`,
        method: 'POST'
      });
    } catch (err) {
      console.error('取消预占失败', err);
    }
  },

  // 🔥 获取座位颜色
  getSeatColor(seat) {
    const { selectedSeatIds } = this.data;

    // 已选 - 橙色
    if (selectedSeatIds.includes(seat.seatId)) {
      return '#fa8c16';
    }

    // 已售 - 红色
    if (seat.seatStatus === 1) {
      return '#ff4d4f';
    }

    // 可选 - 绿色
    return '#52c41a';
  },

  // 获取座位类名
  getSeatClass(seat) {
    const { selectedSeatIds } = this.data;

    if (selectedSeatIds.includes(seat.seatId)) {
      return 'seat-selected';
    }

    if (seat.seatStatus === 1) {
      return 'seat-sold';
    }

    return 'seat-available';
  },

  formatShowTime(timeStr) {
    if (!timeStr) return '';
    const match = timeStr.match(/\d{2}:\d{2}/);
    return match ? match[0] : timeStr;
  },

  getTotalPrice() {
    const total = this.data.price * this.data.selectedSeatIds.length;
    return Number.isInteger(total) ? total : total.toFixed(1);
  },

  confirmSelect() {
    const { selectedSeatIds, movieName, hallName, startTime, price, scheduleId } = this.data;

    if (selectedSeatIds.length === 0) {
      wx.showToast({ title: '请先选择座位', icon: 'none', duration: 1500 });
      return;
    }

    wx.navigateTo({
      url: `/pages/order/order-confirm?` +
        `scheduleId=${scheduleId}` +
        `&seatIds=${encodeURIComponent(JSON.stringify(selectedSeatIds))}` +
        `&movieName=${encodeURIComponent(movieName)}` +
        `&hallName=${encodeURIComponent(hallName)}` +
        `&startTime=${encodeURIComponent(startTime)}` +
        `&price=${price}`
    });
  },

  getCurrentUserId() {
    try {
      const storedUser = wx.getStorageSync('userInfo');
      if (!storedUser) {
        return null;
      }

      const user = typeof storedUser === 'string' ? JSON.parse(storedUser) : storedUser;
      const userId = user?.id ?? user?.userId;
      return userId != null ? Number(userId) : null;
    } catch (e) {
      return null;
    }
  }
});
