import request from '@/utils/request'

// 获取场次座位情况
export function getSeatCondition(scheduleId) {
  return request({
    url: `/seatSchedule/public/query/seatCondition/${scheduleId}`,
    method: 'get'
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

// 获取座位合法性
export function validateSeats(data) {
  return request({
    url: '/seatSchedule/public/valid',
    method: 'post',
    data
  })
}