const BASE_URL = {
  AUTH: 'http://127.0.0.1:8000',
  MOVIE: 'http://127.0.0.1:8001',
  SCHEDULE: 'http://127.0.0.1:8002',
  ORDER: 'http://127.0.0.1:8003',
  AI: 'http://127.0.0.1:8004',
  USER: 'http://127.0.0.1:8000'
};

function request(options) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');

    // 处理表单数据
    let data = options.data;
    let contentType = options.header?.['Content-Type'] || 'application/json';

    if (contentType === 'application/x-www-form-urlencoded') {
      // 将对象转换为 URL 编码格式
      data = Object.keys(data)
        .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(data[key])}`)
        .join('&');
    }

    wx.request({
      url: options.url,
      method: options.method || 'GET',
      data: data,
      header: {
        'Content-Type': contentType,
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        if (res.statusCode === 200 && (res.data.code === 200 || res.data.code === "200")) {
          resolve(res.data);
        } else if (res.statusCode === 401) {
          wx.removeStorageSync('token');
          wx.reLaunch({ url: '/pages/login/login' });
          reject(new Error('未授权'));
        } else {
          wx.showToast({ title: res.data.msg || '请求失败', icon: 'none' });
          reject(res.data);
        }
      },
      fail: (err) => {
        wx.showToast({ title: '网络错误', icon: 'none' });
        reject(err);
      }
    });
  });
}

module.exports = { request, BASE_URL };