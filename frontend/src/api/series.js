/**
 * 系列 API
 */
import request from './request'

/** 创建系列 */
export function createSeries(data) {
  return request.post('/series', data)
}

/** 查询当前用户的所有系列 */
export function listSeries() {
  return request.get('/series')
}

/** 系列详情(含画布列表) */
export function getSeriesDetail(id) {
  return request.get(`/series/${id}`)
}

/** 更新系列 */
export function updateSeries(id, data) {
  return request.put(`/series/${id}`, data)
}

/** 删除系列 */
export function deleteSeries(id) {
  return request.delete(`/series/${id}`)
}
