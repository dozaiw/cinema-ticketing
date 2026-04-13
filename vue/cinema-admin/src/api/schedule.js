import request from '@/utils/request'

// 查询所有排片
export function getAllSchedule() {
  return request({
    url: `/schedule/getAllSchedule`,
    method: 'get',
  })
}

// 管理员分页过滤排片
export function getFilteredScheduleList(params) {
  // params: movieId, cinemaId, date, startTime, endTime, status, displayStatus
  return request({
    url: '/schedule/admin/list/filtered',
    method: 'get',
    params
  })
}

// 查询某电影当天所有排片
export function getSchedulesByMovieAndDate(movieId, date) {
  return request({
    url: `/schedule/public/${movieId}/schedules`,
    method: 'get',
    params: { date }
  })
}

// 查询某电影未来所有排片
export function getFutureSchedules(movieId) {
  return request({
    url: `/schedule/public/${movieId}/schedules/all`,
    method: 'get'
  })
}

// 查询某电影在某一天的哪些影院有排片
export function getCinemasByMovieAndDate(movieId, date) {
  return request({
    url: `/schedule/public/${movieId}/date/${date}/cinemas`,
    method: 'get'
  })
}

// 查询某电影在某影院某一天的所有排片
export function getSchedulesByMovieCinemaAndDate(movieId, cinemaId, date) {
  return request({
    url: `/schedule/public/movie/${movieId}/cinema/${cinemaId}/date/${date}`,
    method: 'get'
  })
}

// 增加排片
export function addSchedule(data) {
  return request({
    url: '/schedule/admin/add',
    method: 'post',
    data
  })
}

// 修改排片
export function updateSchedule(data) {
  return request({
    url: '/schedule/admin/update',
    method: 'post',
    data
  })
}

// 删除排片
export function deleteSchedule(id) {
  return request({
    url: `/schedule/admin/delete/${id}`,
    method: 'post'
  })
}