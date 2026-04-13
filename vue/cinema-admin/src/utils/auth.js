const TOKEN_KEY = 'cinema_admin_token'
const USER_INFO_KEY = 'cinema_admin_user'

/**
 * 获取 Token
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 设置 Token
 */
export function setToken(token) {
  return localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 移除 Token
 */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)
}

/**
 * 获取用户信息
 */
export function getUserInfo() {
  const info = localStorage.getItem(USER_INFO_KEY)
  return info ? JSON.parse(info) : null
}

/**
 * 设置用户信息
 */
export function setUserInfo(userInfo) {
  return localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
}

/**
 * 判断是否登录
 */
export function isLogin() {
  return !!getToken()
}

/**
 * 获取用户角色
 */
export function getUserRoles() {
  const userInfo = getUserInfo()
  return userInfo?.authorities?.map(item => item.authority) || []
}

/**
 * 判断是否有某个角色
 */
export function hasRole(role) {
  const roles = getUserRoles()
  return roles.includes(role)
}

/**
 * 判断是否有某些角色（满足任一）
 */
export function hasAnyRole(roles) {
  const userRoles = getUserRoles()
  return roles.some(role => userRoles.includes(role))
}