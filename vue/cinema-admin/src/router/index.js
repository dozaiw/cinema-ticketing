import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/register.vue'),
    meta: { title: '注册', hidden: true }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限', hidden: true }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', hidden: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'Odometer' }
      },
      // ✅ 影院管理 - 改为子路由结构
      {
        path: 'cinema',
        name: 'Cinema',
        meta: { title: '影院管理', icon: 'OfficeBuilding', roles: ['admin'] },
        redirect: '/cinema/list',
        children: [
          {
            path: 'list',
            name: 'CinemaList',
            component: () => import('@/views/cinema/list/index.vue'),
            meta: { title: '影院列表', icon: 'OfficeBuilding', roles: ['admin'] }
          },
          {
            path: 'hall',
            name: 'CinemaHall',
            component: () => import('@/views/cinema/hall/index.vue'),
            meta: { title: '影厅管理', icon: 'Grid', roles: ['admin'] }
          }
        ]
      },
      // ✅ 电影管理 - 改为子路由结构
      {
        path: 'movie',
        name: 'Movie',
        meta: { title: '电影管理', icon: 'Film', roles: ['admin'] },
        redirect: '/movie/list',
        children: [
          {
            path: 'list',
            name: 'MovieList',
            component: () => import('@/views/movie/list/index.vue'),
            meta: { title: '电影列表', icon: 'Film', roles: ['admin'] }
          },
          {
            path: 'genre',
            name: 'MovieGenre',
            component: () => import('@/views/movie/genre/index.vue'),
            meta: { title: '类型管理', icon: 'List', roles: ['admin'] }
          },
          {
            path: 'actor',
            name: 'MovieActor',
            component: () => import('@/views/movie/actor/index.vue'),
            meta: { title: '演员管理', icon: 'User', roles: ['admin'] }
          },
          {
            path: 'comment',
            name: 'MovieComment',
            component: () => import('@/views/movie/comment/index.vue'),
            meta: { title: '评论审核', icon: 'ChatLineRound', roles: ['admin'] }
          }
        ]
      },
      {
        path: 'schedule',
        name: 'Schedule',
        component: () => import('@/views/schedule/index.vue'),
        meta: { title: '排片管理', icon: 'Calendar', roles: ['admin'] }
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('@/views/order/index.vue'),
        meta: { title: '订单管理', icon: 'Document', roles: ['admin'] }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理', icon: 'User', roles: ['admin'] }
      },  
      {
        path: 'advertisement',
        name: 'Advertisement',  
        component: () => import('@/views/advertisement/index.vue'),  // ← 加上逗号
        meta: { 
          title: '广告管理', 
          icon: 'Picture',       // Element Plus 内置图标，'Advertisement' 不存在
          roles: ['admin'] 
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局路由守卫 - 权限控制
router.beforeEach(async (to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 影院管理系统` : '影院管理系统'

  const userStore = useUserStore()
  const token = userStore.token
  const hasToken = !!token
  const whiteList = ['/login', '/register', '/403', '/404']

  if (hasToken) {
    if (to.path === '/login' || to.path === '/register') {
      next({ path: '/' })
    } else {
      // 🔐 角色权限校验（如果路由配置了 roles）
      if (to.meta.roles && to.meta.roles.length > 0) {
        const userRoles = userStore.roles?.length
          ? userStore.roles
          : (userStore.userInfo?.authorities?.map(item => item.authority) || [])
        const hasPermission = to.meta.roles.some(role => userRoles.includes(role))
        if (!hasPermission) {
          next('/403')
          return
        }
      }
      next()
    }
  } else {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next(`/login?redirect=${to.fullPath}`)
    }
  }
})

export default router
