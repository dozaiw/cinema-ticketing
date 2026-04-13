import request from '@/utils/request'

// 查询电影类型
export function getGenreList() {
  return request({
    url: '/genre/list',
    method: 'get'
  })
}

// 添加电影类型
export function addGenre(data) {
  return request({
    url: '/genre/add',
    method: 'post',
    data
  })
}

// 修改类型信息
export function updateGenre(data) {
  return request({
    url: '/genre/update',
    method: 'post',
    data
  })
}

// 删除类型
export function deleteGenre(id) {
  return request({
    url: `/genre/delete/${id}`,
    method: 'post'
  })
}