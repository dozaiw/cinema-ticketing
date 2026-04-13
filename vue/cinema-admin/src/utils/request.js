import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getToken, removeToken } from '@/utils/auth'

// ⭐ 创建 axios 实例（baseURL 设为空，使用 Vite 代理）
const request = axios.create({
  baseURL: '',  // ⭐ 关键：设为空字符串
  timeout: 10000,
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 🔧 防止 304 缓存
    if (config.method && config.method.toLowerCase() === 'get') {
      config.headers['Cache-Control'] = 'no-cache'
      config.headers.Pragma = 'no-cache'
      // 或者通过参数加时间戳
      config.params = config.params || {}
      config.params._t = Date.now()
    }

    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器（保持不变）
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      if (res.code === 401 || res.code === 403) {
        removeToken()
        router.push('/login')
      }
      return Promise.reject(new Error(res.msg || 'Error'))
    }
    return res
  },
  error => {
    if (error.message.includes('Network Error')) {
      ElMessage.error('网络连接失败，请检查服务器')
    } else if (error.message.includes('timeout')) {
      ElMessage.error('请求超时，请重试')
    } else if (error.response) {
      const status = error.response.status
      const msgMap = {
        400: '请求参数错误',
        401: '未授权，请重新登录',
        403: '权限不足',
        404: '资源不存在',
        500: '服务器内部错误'
      }
      ElMessage.error(msgMap[status] || '请求失败')
      if (status === 401) {
        removeToken()
        router.push('/login')
      }
    }
    return Promise.reject(error)
  }
)

export default request

export const getAdvertisementPage = (params) => {
  return request({
    url: '/advertisement/page',
    method: 'get',
    params: {
      pageNum: 1,
      pageSize: 10,
      _t: Date.now(),          // 加上防缓存字段
      ...params
    }
  })
}