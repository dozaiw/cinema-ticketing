export default {
  // 系统标题
  title: '电影院后台管理系统',
  
  // 是否显示设置
  showSettings: false,
  
  // 是否显示多标签页
  tagsView: true,
  
  // 是否固定头部
  fixedHeader: true,
  
  // 是否显示侧边栏 Logo
  sidebarLogo: true,
  
  // 主题颜色
  theme: '#409EFF',
  
  // 分页大小
  pageSize: 10,
  
  // 分页大小选项
  pageSizes: [10, 20, 50, 100],
  
  // Token 存储键名
  tokenKey: 'cinema_admin_token',
  
  // 用户信息存储键名
  userInfoKey: 'cinema_admin_user',
  
  // 请求超时时间
  timeout: 10000,
  
  // API 基础路径
  baseURL: {
    auth: import.meta.env.VITE_APP_AUTH_API || 'http://127.0.0.1:8000',
    movie: import.meta.env.VITE_APP_BASE_API || 'http://127.0.0.1:8001',
    schedule: import.meta.env.VITE_APP_SCHEDULE_API || 'http://127.0.0.1:8002',
    order: import.meta.env.VITE_APP_ORDER_API || 'http://127.0.0.1:8003'
  }
}

// 获取广告页数据
const res = await request({
  url: '/advertisement/page',
  method: 'get',
  params: {
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    title: searchForm.title || undefined,
    status: searchForm.status,
    _t: Date.now()
  },
  headers: {
    'Cache-Control': 'no-store',
    Pragma: 'no-cache'
  }
})