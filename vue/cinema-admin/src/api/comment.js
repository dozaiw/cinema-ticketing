import request from '@/utils/request'

export function getCommentReportPage(params) {
  return request({
    url: '/comment/admin/report/list',
    method: 'get',
    params
  })
}

export function handleCommentReport(data) {
  return request({
    url: '/comment/admin/report/handle',
    method: 'post',
    data
  })
}
