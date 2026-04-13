import request from '@/utils/request'

/**
 * 获取热映电影数量
 */
export function getHotMovieCount() {
  return request({
    url: '/movie/public/hot/count',
    method: 'get'
  })
}

/**
 * 获取营业影院数量
 */
export function getWorkingCinemaCount() {
  return request({
    url: '/cinema/getWorkingCinemaCount',
    method: 'get'
  })
}

/**
 * 获取今日排片数量
 * @param {string} date - 日期格式：yyyy-MM-dd
 */
export function getTodayScheduleCount(date) {
  return request({
    url: `/schedule/getTodaySchedule/${date}`,
    method: 'get'
  })
}

/**
 * 获取今日订单数量
 * @param {string} date - 日期格式：yyyy-MM-dd
 */
export function getTodayOrderCount(date) {
  return request({
    url: `/order/queryTodayOrderCount/${date}`,
    method: 'get'
  })
}

/**
 * 获取当日电影销售统计
 * @param {string} date - 日期格式：yyyy-MM-dd
 */
export function getMovieSalesStats(date) {
  return request({
   url: '/order/getMovieSalesStats',
   method: 'get',
   params: { date}
  })
}

/**
 * 获取当日影厅销售统计
 * @param {string} date - 日期格式：yyyy-MM-dd
 */
export function getHallSalesStats(date) {
  return request({
   url: '/order/getHallSalesStats',
   method: 'get',
   params: { date}
  })
}

/**
 * 获取当日订单状态分布
 * @param {string} date - 日期格式：yyyy-MM-dd
 */
export function getOrderStatusStats(date) {
  return request({
   url: '/order/getOrderStatusStats',
   method: 'get',
   params: { date }
  })
}