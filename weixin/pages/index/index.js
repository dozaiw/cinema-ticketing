const { request, BASE_URL } = require('../../utils/request');

Page({
  data: {
    advertisementList: [],
    hotList: [],
    waitList: [],
    searchResult: [],
    searchKeyword: '',
    isSearching: false,
    currentTab: 'hot',  // 当前标签：hot 或 wait
    pageNum: 1,
    pageSize: 10,
    loading: false,
    noMore: false
  },

  onLoad() {
    this.loadAdvertisement();
    this.loadHotMovies(true);
  },

  // 加载广告
  async loadAdvertisement() {
    try {
      const res = await request({
        url: `${BASE_URL.MOVIE}/advertisement/list/enabled`,
        method: 'GET'
      });
      if (res.code === 200 || res.code === "200") {
        this.setData({ advertisementList: res.data || [] });
      }
    } catch (err) {
      console.error('加载广告失败:', err);
    }
  },

  // 广告点击
  onAdTap(e) {
    const linkUrl = e.currentTarget.dataset.link;
    const movieId = e.currentTarget.dataset.movieId;
    if (movieId) {
      wx.navigateTo({ url: `/pages/detail/detail?id=${movieId}` });
    }
  },

  onReachBottom() {
    if (!this.data.loading && !this.data.noMore && !this.data.isSearching) {
      if (this.data.currentTab === 'hot') {
        this.loadHotMovies(false);
      } else {
        this.loadWaitMovies(false);
      }
    }
  },

  // 切换标签
  switchTab(e) {
    const type = e.currentTarget.dataset.type;
    if (this.data.currentTab === type) return;

    this.setData({
      currentTab: type,
      isSearching: false,
      searchKeyword: '',
      searchResult: []
    });

    // 加载对应列表
    if (type === 'hot') {
      this.loadHotMovies(true);
    } else {
      this.loadWaitMovies(true);
    }
  },

  // 搜索输入
  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value });
  },

  // 执行搜索
  async onSearch() {
    const keyword = this.data.searchKeyword.trim();
    if (!keyword) {
      wx.showToast({ title: '请输入搜索内容', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '搜索中...' });

    try {
      const res = await request({
        url: `${BASE_URL.MOVIE}/movie/public/find/ByName`,
        method: 'GET',
        data: { name: keyword }
      });

      wx.hideLoading();

      if (res.code === 200 || res.code === "200") {
        const searchResult = res.data.records || [];
        this.setData({
          searchResult: searchResult,
          isSearching: true
        });

        if (searchResult.length === 0) {
          wx.showToast({ title: '未找到相关电影', icon: 'none' });
        }
      } else {
        wx.showToast({ title: res.msg || '搜索失败', icon: 'none' });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '搜索失败', icon: 'none' });
      console.error(err);
    }
  },

  // 清除搜索
  clearSearch() {
    this.setData({
      searchKeyword: '',
      searchResult: [],
      isSearching: false
    });
  },

  // 加载热映电影
  async loadHotMovies(isRefresh = false) {
    if (this.data.loading) return;

    this.setData({ loading: true });

    try {
      const pageNum = isRefresh ? 1 : this.data.pageNum + 1;
      const res = await request({
        url: `${BASE_URL.MOVIE}/movie/public/hot/list`,
        method: 'GET',
        data: {
          pageNum: pageNum,
          pageSize: this.data.pageSize
        }
      });

      if (res.code === 200 || res.code === "200") {
        const newList = res.data.records || [];
        const total = res.data.total || 0;
        const noMore = newList.length < this.data.pageSize || (pageNum * this.data.pageSize >= total);

        this.setData({
          hotList: isRefresh ? newList : [...this.data.hotList, ...newList],
          pageNum: pageNum,
          noMore: noMore,
          loading: false
        });
      }
    } catch (err) {
      this.setData({ loading: false });
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },

  // 加载待映电影
  async loadWaitMovies(isRefresh = false) {
    if (this.data.loading) return;

    this.setData({ loading: true });

    try {
      const pageNum = isRefresh ? 1 : this.data.pageNum + 1;
      const res = await request({
        url: `${BASE_URL.MOVIE}/movie/public/wait/list`,
        method: 'GET',
        data: {
          pageNum: pageNum,
          pageSize: this.data.pageSize
        }
      });

      if (res.code === 200 || res.code === "200") {
        const newList = res.data.records || [];
        const total = res.data.total || 0;
        const noMore = newList.length < this.data.pageSize || (pageNum * this.data.pageSize >= total);

        this.setData({
          waitList: isRefresh ? newList : [...this.data.waitList, ...newList],
          pageNum: pageNum,
          noMore: noMore,
          loading: false
        });
      }
    } catch (err) {
      this.setData({ loading: false });
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },

  // 日期格式化
  formatDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },

  // 跳转详情
  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/detail/detail?id=${id}` });
  },

  // 跳转购票
  goBuy(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/schedule/schedule?movieId=${id}` });
  },

  // 跳转附近影院
  goNearby() {
    wx.navigateTo({ url: '/pages/nearby/nearby' });
  },

  // 跳转AI助手
  goAIChat() {
    wx.navigateTo({ url: '/pages/ai-chat/ai-chat' });
  },

  // 全部热映
  goAllHot() {
    wx.showToast({ title: '全部热映开发中', icon: 'none' });
  }
});