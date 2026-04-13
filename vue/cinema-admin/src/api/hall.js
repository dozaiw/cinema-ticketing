// src/api/hall.js
import request from '@/utils/request'

/**
 * 根据影院ID获取影厅列表
 * @param {number|string} cinemaId - 影院ID
 */
export function getHallsByCinema(cinemaId) {
  return request({
    url: `/hall/admin/${cinemaId}/halls`,
    method: 'get'
  })
}

/**
 * 新增影厅
 * @param {number|string} cinemaId - 影院ID（URL参数）
 * @param {Object} data - 影厅信息 { name, seatRows, seatCols }
 */
export function addHall(cinemaId, data) {
  return request({
    url: `/hall/admin/add/${cinemaId}`,
    method: 'post',
    data
  })
}

/**
 * 修改影厅信息
 * @param {Object} data - 影厅信息 { id, name, seatRows, seatCols }
 */
export function updateHall(data) {
  return request({
    url: '/hall/admin/update',
    method: 'post',
    data
  })
}

/**
 * 删除影厅
 * @param {number|string} hallId - 影厅ID
 */
export function deleteHall(hallId) {
  return request({
    url: `/hall/admin/delete/${hallId}`,
    method: 'post'
  })
}

/**
 * 初始化影厅座位
 * @param {number|string} hallId - 影厅ID
 */
export function initHallSeats(hallId) {
  return request({
    url: `/seat/admin/init/${hallId}`,
    method: 'post'
  })
}