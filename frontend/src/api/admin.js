import request from './request'

/**
 * 获取待终审列表(运营后台)
 */
export function getPendingReviews() {
  return request({
    url: '/orders/admin/reviews/pending',
    method: 'get'
  })
}

/**
 * 获取待报价列表(运营后台)
 */
export function getPendingQuotes() {
  return request({
    url: '/orders/admin/quotes/pending',
    method: 'get'
  })
}

/**
 * 终审操作(运营后台)
 */
export function reviewOrder(orderId, data) {
  return request({
    url: `/orders/admin/reviews/${orderId}`,
    method: 'post',
    data
  })
}

/**
 * 报价(运营后台)
 */
export function quoteOrder(orderId, data) {
  return request({
    url: `/orders/admin/orders/${orderId}/quote`,
    method: 'post',
    data
  })
}

/**
 * 获取订单详情(运营后台)
 */
export function getAdminOrderDetail(orderId) {
  return request({
    url: `/orders/${orderId}`,
    method: 'get'
  })
}

/**
 * 获取运营看板统计数据
 */
export function getDashboardStats() {
  return request({
    url: '/orders/admin/dashboard/stats',
    method: 'get'
  })
}
