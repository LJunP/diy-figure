import request from './request'

/**
 * 创建订单
 */
export function createOrder(data) {
  return request({
    url: '/orders',
    method: 'post',
    data
  })
}

/**
 * 获取订单列表
 */
export function getOrderList() {
  return request({
    url: '/orders',
    method: 'get'
  })
}

/**
 * 获取订单详情
 */
export function getOrderDetail(orderId) {
  return request({
    url: `/orders/${orderId}`,
    method: 'get'
  })
}

/**
 * 接受报价
 */
export function acceptQuote(orderId) {
  return request({
    url: `/orders/${orderId}/accept-quote`,
    method: 'post'
  })
}

/**
 * 拒绝报价
 */
export function rejectQuote(orderId) {
  return request({
    url: `/orders/${orderId}/reject-quote`,
    method: 'post'
  })
}

/**
 * 执行抽奖
 */
export function drawLottery(orderId) {
  return request({
    url: `/orders/${orderId}/lottery/draw`,
    method: 'post'
  })
}

/**
 * 绑定收货地址
 */
export function bindOrderAddress(orderId, addressId) {
  return request({
    url: `/orders/${orderId}/address`,
    method: 'post',
    data: { addressId }
  })
}
