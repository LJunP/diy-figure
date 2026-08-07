/**
 * 收货地址 API
 */
import request from './request'

/** 查询当前用户的所有收货地址 */
export function listAddresses() {
  return request.get('/addresses')
}

/** 查询单个地址详情 */
export function getAddress(id) {
  return request.get(`/addresses/${id}`)
}

/** 新增收货地址 */
export function createAddress(data) {
  return request.post('/addresses', data)
}

/** 修改收货地址 */
export function updateAddress(id, data) {
  return request.put(`/addresses/${id}`, data)
}

/** 删除收货地址 */
export function deleteAddress(id) {
  return request.delete(`/addresses/${id}`)
}
