import request from '@/utils/request'

// 批量添加电影人员关联
export function batchAddMovieStaff(data) {
  return request({
    url: '/movie-staff/batch-add',
    method: 'post',
    data  // 数组格式：[{ movieId, actorId, role, characterName }]
  })
}

// 根据电影ID查询所有人员
export function getStaffByMovie(movieId) {
  return request({
    url: `/movie-staff/movie/${movieId}`,
    method: 'get'
  })
  // 返回 data.staffList: [{ actorId, actorName, role, characterName, ... }]
}

// 根据电影ID和演员ID删除关联
export function deleteMovieStaff(movieId, actorId) {
  return request({
    url: '/movie-staff/delete',
    method: 'delete',
    params: { movieId, actorId }  // Query参数
  })
}