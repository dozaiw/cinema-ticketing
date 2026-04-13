function parseStoredUser(rawUserInfo) {
  if (!rawUserInfo) {
    return null
  }

  if (typeof rawUserInfo === 'string') {
    try {
      return JSON.parse(rawUserInfo)
    } catch (error) {
      return null
    }
  }

  return rawUserInfo
}

function normalizeAuthorities(authorities) {
  if (!Array.isArray(authorities)) {
    return []
  }

  return authorities
    .map(item => {
      if (!item) {
        return ''
      }

      if (typeof item === 'string') {
        return item
      }

      return item.authority || ''
    })
    .filter(Boolean)
}

function normalizeUserInfo(userData) {
  const authorities = normalizeAuthorities(userData?.authorities)
  const role = userData?.role != null ? Number(userData.role) : (authorities.includes('admin') ? 0 : 1)
  const admin = role === 0 || authorities.includes('admin')

  return {
    id: userData?.id ?? userData?.userId ?? null,
    userId: userData?.userId ?? userData?.id ?? null,
    username: userData?.username || '',
    phone: userData?.phone || '',
    nickname: userData?.nickname || (admin ? '管理员' : '用户'),
    avatar: userData?.avatar || '',
    role,
    authorities,
    isAdmin: admin
  }
}

function getToken() {
  return wx.getStorageSync('token') || ''
}

function getUserInfo() {
  return parseStoredUser(wx.getStorageSync('userInfo'))
}

function saveUserInfo(userData) {
  const userInfo = normalizeUserInfo(userData)
  wx.setStorageSync('userInfo', JSON.stringify(userInfo))
  return userInfo
}

function clearAuth() {
  wx.removeStorageSync('token')
  wx.removeStorageSync('userInfo')
}

function isAdmin(userInfo) {
  const currentUser = userInfo || getUserInfo()
  if (!currentUser) {
    return false
  }

  const authorities = normalizeAuthorities(currentUser.authorities)
  return currentUser.role === 0 || currentUser.isAdmin === true || authorities.includes('admin')
}

module.exports = {
  clearAuth,
  getToken,
  getUserInfo,
  isAdmin,
  normalizeUserInfo,
  saveUserInfo
}
