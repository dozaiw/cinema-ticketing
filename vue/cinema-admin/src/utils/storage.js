/**
 * 设置 localStorage
 */
export function setLocal(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch (error) {
    console.error('localStorage 设置失败:', error)
  }
}

/**
 * 获取 localStorage
 */
export function getLocal(key) {
  try {
    const value = localStorage.getItem(key)
    return value ? JSON.parse(value) : null
  } catch (error) {
    console.error('localStorage 获取失败:', error)
    return null
  }
}

/**
 * 移除 localStorage
 */
export function removeLocal(key) {
  try {
    localStorage.removeItem(key)
  } catch (error) {
    console.error('localStorage 移除失败:', error)
  }
}

/**
 * 清空 localStorage
 */
export function clearLocal() {
  try {
    localStorage.clear()
  } catch (error) {
    console.error('localStorage 清空失败:', error)
  }
}

/**
 * 设置 sessionStorage
 */
export function setSession(key, value) {
  try {
    sessionStorage.setItem(key, JSON.stringify(value))
  } catch (error) {
    console.error('sessionStorage 设置失败:', error)
  }
}

/**
 * 获取 sessionStorage
 */
export function getSession(key) {
  try {
    const value = sessionStorage.getItem(key)
    return value ? JSON.parse(value) : null
  } catch (error) {
    console.error('sessionStorage 获取失败:', error)
    return null
  }
}

/**
 * 移除 sessionStorage
 */
export function removeSession(key) {
  try {
    sessionStorage.removeItem(key)
  } catch (error) {
    console.error('sessionStorage 移除失败:', error)
  }
}

/**
 * 清空 sessionStorage
 */
export function clearSession() {
  try {
    sessionStorage.clear()
  } catch (error) {
    console.error('sessionStorage 清空失败:', error)
  }
}