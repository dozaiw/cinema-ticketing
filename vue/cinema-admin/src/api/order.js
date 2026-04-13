import request from '@/utils/request'

// 创建订单
export function createOrder(data) {
  return request({
    url: '/order/create',
    method: 'post',
    data
  })
}

// 支付订单
export function payOrder(orderId, passWord) {
  return request({
    url: '/order/pay',
    method: 'post',
    params: { orderId, passWord }
  })
}

// 取消订单
export function cancelOrder(orderId) {
  return request({
    url: '/order/cancel',
    method: 'post',
    params: { orderId }
  })
}

// 查询用户所有订单
export function queryAllOrder() {
  return request({
    url: '/order/adminQueryAllOrder',
    method: 'get'
  })
}

// 核销订单
export function verifyTicket(verifyCode) {
  return request({
    url: '/order/verifyTicket',
    method: 'post',
    params: { verifyCode }
  })
}

// 获取座位合法性
export function validateSeats(data) {
  return request({
    url: '/seatSchedule/public/valid',
    method: 'post',
    data
  })
}

// 预定座位
export function preselectSeat(data) {
  return request({
    url: '/seatSchedule/preselectSeat',
    method: 'post',
    data
  })
}

// 取消锁座
export function cancelSeat(scheduleId, seatId) {
  return request({
    url: `/seatSchedule/cancelSeat/${scheduleId}/${seatId}`,
    method: 'post'
  })
}

// 获取场次座位情况
export function getSeatCondition(scheduleId) {
  return request({
    url: `/seatSchedule/public/query/seatCondition/${scheduleId}`,
    method: 'get'
  })
}
