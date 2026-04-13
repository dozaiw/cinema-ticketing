import request from '@/utils/request'
import { method } from 'lodash-es'

// 获取热映电影
export function getHotMovies(params) {
  return request({
    url: '/movie/public/hot/list',
    method: 'get',
    params
  })
}

// 获取待映电影
export function getWaitMovies(params) {
  return request({
    url: '/movie/public/wait/list',
    method: 'get',
    params
  })
}

// 根据名称查询电影
export function searchMovieByName(name) {
  return request({
    url: '/movie/public/find/ByName',
    method: 'get',
    params: { name }
  })
}

// 根据类型查询电影
export function searchMovieByGenre(genre) {
  return request({
    url: '/movie/public/find/ByGenre',
    method: 'get',
    params: { genre }
  })
}

// 更改电影状态
export function changeMovieState(data) {
  return request({
    url: '/movie/admin/changeState',
    method: 'post',
    data
  })
}

// 管理员修改电影
export function updateMovie(data) {
  return request({
    url: '/movie/admin/changeMovie',
    method: 'post',
    data
  })
}

// 管理员增加电影
export function addMovie(data) {
  return request({
    url: '/movie/admin/add',
    method: 'post',
    data
  })
}

// 获得所有电影（支持分页/过滤）
// 请求使用 params 而不是 data，以便 axios 在 GET 请求中正确序列化
export function getAllMovie(params) {
  return request({
    url: '/movie/public/getAllMovie',
    method: 'get',
    params
  })
}

// 获取热映电影数量
export function getHotMovieCount(data){
  return request({
    url: 'movie/public/hot/count',
    method: 'get',
    data
  })
}

/**
 * 删除电影
 */
export function deleteMovie(id) {
  return request({
    url: `/movie/admin/delete/${id}`,
    method: 'delete'
  })
}