import request from '@/utils/request'

/**
 * 添加演员
 */
export function addActor(data) {
  return request({
   url: '/actor/add',
   method: 'post',
   data
  })
}

/**
 * 修改演员
 */
export function updateActor(data) {
  return request({
   url: '/actor/update',
   method: 'post',
   data
  })
}

/**
 * 删除演员
 */
export function deleteActor(id) {
  return request({
   url: `/actor/delete/${id}`,
   method: 'delete'
  })
}

/**
 * 根据 ID 查询演员
 */
export function getActorById(id) {
  return request({
   url: `/actor/get/${id}`,
   method: 'get'
  })
}

/**
 * 根据姓名查询演员（模糊搜索）
 */
export function searchActor(name) {
  return request({
   url: '/actor/search',
   method: 'get',
   params: { name }
  })
}

/**
 * 分页查询演员列表
 */
export function getActorPage(params) {
  return request({
  url: '/actor/page',
  method: 'get',
  params
  })
}

/**
 * 分页查询演员列表（带过滤条件）
 */
export function getActorPageFiltered(params) {
  return request({
  url: '/actor/page/filtered',
  method: 'get',
  params
  })
}