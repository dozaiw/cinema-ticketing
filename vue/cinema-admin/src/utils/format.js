import dayjs from 'dayjs'

/**
 * 格式化日期
 */
export function formatDate(date, format = 'YYYY-MM-DD') {
  if (!date) return '-'
  return dayjs(date).format(format)
}

/**
 * 格式化时间
 */
export function formatTime(date, format = 'HH:mm:ss') {
  if (!date) return '-'
  return dayjs(date).format(format)
}

/**
 * 格式化日期时间
 */
export function formatDateTime(date, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!date) return '-'
  return dayjs(date).format(format)
}

/**
 * 格式化金额
 */
export function formatMoney(amount, decimals = 2) {
  if (amount === null || amount === undefined) return '-'
  return Number(amount).toFixed(decimals).replace(/\d{1,3}(?=(\d{3})+(\.\d*)?$)/g, '$&,')
}

/**
 * 格式化文件大小
 */
export function formatFileSize(bytes) {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

/**
 * 格式化百分比
 */
export function formatPercent(value, decimals = 2) {
  if (value === null || value === undefined) return '-'
  return (value * 100).toFixed(decimals) + '%'
}

/**
 * 格式化手机号（中间隐藏）
 */
export function formatPhone(phone) {
  if (!phone) return '-'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

/**
 * 格式化身份证（中间隐藏）
 */
export function formatIdCard(idCard) {
  if (!idCard) return '-'
  return idCard.replace(/(\d{6})\d{8}(\d{4})/, '$1********$2')
}

/**
 * 格式化座位名称
 */
export function formatSeatName(row, col) {
  return `${row}排${col}座`
}

/**
 * 格式化订单状态
 */
export function formatOrderStatus(status) {
  const statusMap = {
    'PENDING': '待支付',
    'PAID': '已支付',
    'CANCELED': '已取消',
    'REFUNDED': '已退款',
    'COMPLETED': '已完成'
  }
  return statusMap[status] || status
}

/**
 * 格式化电影状态
 */
export function formatMovieStatus(status) {
  const statusMap = {
    0: '已下映',
    1: '热映中',
    2: '待上映'
  }
  return statusMap[status] || '未知'
}

/**
 * 格式化影院状态
 */
export function formatCinemaStatus(status) {
  const statusMap = {
    0: '已停运',
    1: '营业中'
  }
  return statusMap[status] || '未知'
}