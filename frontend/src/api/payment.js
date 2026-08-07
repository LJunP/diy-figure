import request from './request'

/**
 * 创建定金支付
 */
export function createDepositPayment(orderId, channel) {
  return request({
    url: `/payments/orders/${orderId}/deposit`,
    method: 'post',
    data: { channel }
  })
}

/**
 * 创建尾款支付
 */
export function createBalancePayment(orderId, channel) {
  return request({
    url: `/payments/orders/${orderId}/balance`,
    method: 'post',
    data: { channel }
  })
}

/**
 * 查询订单支付记录
 */
export function getPaymentHistory(orderId) {
  return request({
    url: `/payments/orders/${orderId}`,
    method: 'get'
  })
}

/**
 * 模拟支付成功(开发环境)
 */
export function simulatePaymentSuccess(paymentId) {
  return request({
    url: `/payments/${paymentId}/simulate`,
    method: 'post'
  })
}
