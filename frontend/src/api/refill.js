import request from './request'

/** 查看可补购角色列表 */
export function getRefillableCanvases(orderId) {
  return request({ url: `/orders/${orderId}/refill/available`, method: 'get' })
}

/** 发起补购 */
export function createRefillOrder(orderId, data) {
  return request({ url: `/orders/${orderId}/refill`, method: 'post', data })
}

/** 查看我的补购订单列表 */
export function getRefillOrders() {
  return request({ url: '/refills', method: 'get' })
}
