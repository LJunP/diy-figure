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
 * 终审拒绝后重新提交(REVIEW_REJECTED → DRAFT_SUBMIT_PENDING)
 */
export function resubmitOrder(orderId) {
  return request({
    url: `/orders/${orderId}/resubmit`,
    method: 'post'
  })
}

/**
 * 重新打开已关闭订单(CLOSED → DRAFT_SUBMIT_PENDING)
 */
export function reopenOrder(orderId) {
  return request({
    url: `/orders/${orderId}/reopen`,
    method: 'post'
  })
}

/**
 * 提交终审(DRAFT_SUBMIT_PENDING → REVIEWING)
 *
 * 返工闭环的最后一环:resubmit / reopen 只把订单放回草稿态,
 * 必须再调一次这个接口才会重新进入运营的终审队列。
 */
export function submitForReview(orderId) {
  return request({
    url: `/orders/${orderId}/submit-review`,
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
