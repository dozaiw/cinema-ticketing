import request from '@/utils/request'

// 用户登录
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

// 用户注册
export function register(data) {
  const formData = new FormData()
  formData.append('user', JSON.stringify(data))

  return request({
    url: '/auth/regist',
    method: 'post',
    data: formData
  })
}

// 用户登出
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

// 管理员查询单个用户
export function getUserInfo(username) {
  return request({
    url: '/auth/user/info',
    method: 'get',
    params: { username }
  })
}

// 管理员查询用户列表
export function getUserList(params) {
  return request({
    url: '/auth/user/list',
    method: 'get',
    params
  })
}

// 管理员修改用户状态：1 正常，0 封禁
export function changeUserState(username, state) {
  return request({
    url: '/auth/user/admin/changeState',
    method: 'post',
    params: { username, state }
  })
}

// GET 用户列表（过滤）
export function getFilteredUserList(params) {
  // 参数: nickname(模糊), status, role
  return request({
    url: '/auth/user/list/filtered',
    method: 'get',
    params
  })
}

export function getCurrentUserInfo() {
  return request({
    url: '/auth/user/current',  // ⭐ 新接口
    method: 'get'
  })
}
