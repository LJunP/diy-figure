import request from './request'

// ===== 运营端接口 =====

/** 标记生产开工 */
export function markProductionStarted(orderId) {
  return request({ url: `/admin/orders/${orderId}/production-start`, method: 'post' })
}

/** 送检(IN_PRODUCTION → QC_PENDING) */
export function submitForQc(orderId) {
  return request({ url: `/orders/admin/orders/${orderId}/submit-qc`, method: 'post' })
}

/** 质检结果录入 */
export function qualityCheck(orderId, data) {
  return request({ url: `/admin/orders/${orderId}/quality-check`, method: 'post', data })
}

/** 发货 */
export function shipOrder(orderId, data) {
  return request({ url: `/admin/orders/${orderId}/ship`, method: 'post', data })
}

/** 标记订单完成 */
export function completeOrder(orderId) {
  return request({ url: `/admin/orders/${orderId}/complete`, method: 'post' })
}

/** 生产中订单列表 */
export function getProductionList() {
  return request({ url: '/orders/admin/production/list', method: 'get' })
}

/** 待质检列表 */
export function getQcPendingList() {
  return request({ url: '/orders/admin/qc/pending', method: 'get' })
}

/** 已发货列表 */
export function getShippedList() {
  return request({ url: '/orders/admin/shipped/list', method: 'get' })
}

/** 待发货列表 */
export function getPendingShipping() {
  return request({ url: '/orders/admin/shipping/pending', method: 'get' })
}

/** 违约记录列表 */
export function getCancellations() {
  return request({ url: '/admin/cancellations', method: 'get' })
}

/** 质检记录 */
export function getQcLogs(orderId) {
  return request({ url: `/admin/orders/${orderId}/qc-logs`, method: 'get' })
}

// ===== 用户端接口 =====

/** 确认签收 */
export function confirmReceived(orderId) {
  return request({ url: `/orders/${orderId}/confirm-received`, method: 'post' })
}

/** 取消订单 */
export function cancelOrder(orderId) {
  return request({ url: `/orders/${orderId}/cancel`, method: 'post' })
}
