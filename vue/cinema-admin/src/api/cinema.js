import request from '@/utils/request'

// 添加影院
export function addCinema(data) {
  return request({
    url: '/cinema/admin/add',
    method: 'post',
    data
  })
}

// 更新影院
export function updateCinema(data) {
  return request({
    url: '/cinema/admin/update',
    method: 'post',
    data
  })
}

// 更新影院状态
export function changeCinemaStatus(id, status) {
  return request({
    url: '/cinema/admin/changeStatus',
    method: 'post',
    params: { id, status }
  })
}

// 删除影院
export function deleteCinema(id) {
  return request({
    url: `/cinema/admin/delete/${id}`,
    method: 'delete'
  })
}

// 查询影院列表（分页）
export function getCinemaList(params) {
  return request({
    url: '/cinema/admin/list',
    method: 'get',
    params
  })
}

// 查询影院详情
export function getCinemaDetail(id) {
  return request({
    url: `/cinema/admin/${id}`,
    method: 'get'
  })
}

// 查询附近影院
export function getNearbyCinemas(params) {
  return request({
    url: '/cinema/public/nearby',
    method: 'get',
    params
  })
}

// 地址转经纬度
export function geocodeAddress(address) {
  return request({
    url: '/cinema/admin/geocode',
    method: 'post',
    params: { address }
  })
}