import { defineStore } from 'pinia'
import { login, logout, getUserInfo , getCurrentUserInfo } from '@/api/auth'
import { setToken, removeToken, setUserInfo, getUserInfo as getLocalUserInfo } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  // 状态
  state: () => ({
    token: getToken(),
    userInfo: getLocalUserInfo(),
    roles: [],
    permissions: []
  }),
  
  // 计算属性
  getters: {
    // 是否已登录
    isLogin: (state) => !!state.token,
    
    // 用户名
    username: (state) => state.userInfo?.username || '',
    
    // 用户昵称
    nickname: (state) => state.userInfo?.nickname || '',
    
    // 用户头像
    avatar: (state) => state.userInfo?.avatar || '',
    
    // 是否是管理员
    isAdmin: (state) => state.roles?.includes('admin') || false
  },
  
  // 动作
  actions: {
    // 用户登录
    async login(loginForm) {
      try {
        const res = await login(loginForm)
        
        // 保存 Token
        setToken(res.data)
        this.token = res.data
        
        // 获取用户信息
        await this.getInfo()
        
        return res
      } catch (error) {
        console.error('登录失败:', error)
        throw error
      }
    },
    
    // 获取用户信息
    async getInfo() {
      try {
        const res = await getCurrentUserInfo()
        
        this.userInfo = res.data
        this.roles = res.data.authorities?.map(item => item.authority) || []
        
        // 保存用户信息到本地
        setUserInfo(res.data)
        
        return res
      } catch (error) {
        console.error('获取用户信息失败:', error)
        throw error
      }
    },
    
    // 用户登出
    async logout() {
      try {
        await logout()
      } catch (error) {
        console.error('登出失败:', error)
      } finally {
        // 无论成功失败都清除本地数据
        this.resetToken()
      }
    },
    
    // 重置 Token（Token 过期时调用）
    resetToken() {
      removeToken()
      this.token = ''
      this.userInfo = null
      this.roles = []
      this.permissions = []
    },
    
    // 更新用户信息（可选）
    async updateUserInfo() {
      await this.getInfo()
    }
  }
})

// 辅助函数：获取本地 Token
function getToken() {
  return localStorage.getItem('cinema_admin_token')
}