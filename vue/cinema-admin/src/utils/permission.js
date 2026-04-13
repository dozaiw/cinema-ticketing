import { useUserStore } from '@/store/user'

/**
 * 判断是否有某个角色
 */
export function hasRole(role) {
  const userStore = useUserStore()
  return userStore.roles?.includes(role) || false
}

/**
 * 判断是否有某些角色（满足任一）
 */
export function hasAnyRole(roles) {
  const userStore = useUserStore()
  const userRoles = userStore.roles || []
  return roles.some(role => userRoles.includes(role))
}

/**
 * 判断是否有所有角色（满足全部）
 */
export function hasAllRoles(roles) {
  const userStore = useUserStore()
  const userRoles = userStore.roles || []
  return roles.every(role => userRoles.includes(role))
}

/**
 * 判断是否是管理员
 */
export function isAdmin() {
  return hasRole('admin')
}

/**
 * 获取当前用户角色列表
 */
export function getUserRoles() {
  const userStore = useUserStore()
  return userStore.roles || []
}

/**
 * 判断是否有权限访问某个路由
 */
export function canAccessRoute(route) {
  const userStore = useUserStore()
  const roles = route.meta?.roles
  
  // 没有角色限制，所有人都可以访问
  if (!roles || roles.length === 0) {
    return true
  }
  
  // 有角色限制，检查用户是否有对应角色
  return roles.some(role => userStore.roles?.includes(role))
}