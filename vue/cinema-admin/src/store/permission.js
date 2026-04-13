import router from '@/router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({ showSpinner: false })

// 白名单（无需登录即可访问）
const whiteList = ['/login']

router.beforeEach(async (to, from, next) => {
  NProgress.start()
  
  const userStore = useUserStore()
  const hasToken = userStore.token
  
  if (hasToken) {
    if (to.path === '/login') {
      // 已登录，跳转到首页
      next({ path: '/' })
      NProgress.done()
    } else {
      // 检查是否有用户信息
      if (!userStore.userInfo) {
        try {
          await userStore.getInfo()
          next({ ...to, replace: true })
        } catch (error) {
          // Token 过期，重新登录
          await userStore.resetToken()
          ElMessage.error(error.message || '认证失败')
          next(`/login?redirect=${to.path}`)
          NProgress.done()
        }
      } else {
        // 权限检查（可选）
        if (to.meta.roles && to.meta.roles.length > 0) {
          const hasRole = userStore.roles.some(role => 
            to.meta.roles.includes(role)
          )
          if (!hasRole) {
            next({ path: '/403', replace: true })
            NProgress.done()
            return
          }
        }
        next()
      }
    }
  } else {
    // 无 Token
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next(`/login?redirect=${to.path}`)
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})