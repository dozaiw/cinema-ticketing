/**
 * 校验手机号
 */
export function isPhone(value) {
  const reg = /^1[3-9]\d{9}$/
  return reg.test(value)
}

/**
 * 校验邮箱
 */
export function isEmail(value) {
  const reg = /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/
  return reg.test(value)
}

/**
 * 校验身份证
 */
export function isIdCard(value) {
  const reg = /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/
  return reg.test(value)
}

/**
 * 校验 URL
 */
export function isUrl(value) {
  const reg = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/
  return reg.test(value)
}

/**
 * 校验是否为空
 */
export function isEmpty(value) {
  return value === null || value === undefined || value === ''
}

/**
 * 校验长度范围
 */
export function isLengthInRange(value, min, max) {
  const length = value?.length || 0
  return length >= min && length <= max
}

/**
 * 校验密码强度（至少 6 位，包含字母和数字）
 */
export function isPasswordStrong(value) {
  if (!value || value.length < 6) {
    return false
  }
  const hasLetter = /[a-zA-Z]/.test(value)
  const hasNumber = /\d/.test(value)
  return hasLetter && hasNumber
}

/**
 * 校验日期格式
 */
export function isDateFormat(value, format = 'YYYY-MM-DD') {
  if (!value) return false
  // 简单校验 YYYY-MM-DD 格式
  if (format === 'YYYY-MM-DD') {
    const reg = /^\d{4}-\d{2}-\d{2}$/
    return reg.test(value)
  }
  return true
}

/**
 * 校验时间格式
 */
export function isTimeFormat(value) {
  const reg = /^\d{2}:\d{2}(:\d{2})?$/
  return reg.test(value)
}

/**
 * 校验日期时间格式
 */
export function isDateTimeFormat(value) {
  const reg = /^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}(:\d{2})?$/
  return reg.test(value)
}