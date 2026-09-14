import request from './request'

/**
 * 我的站内消息列表
 */
export function listNotifications() {
  return request({
    url: '/notifications',
    method: 'get'
  })
}

/**
 * 未读消息数量
 */
export function getUnreadCount() {
  return request({
    url: '/notifications/unread-count',
    method: 'get'
  })
}

/**
 * 标记单条已读
 */
export function markNotificationRead(id) {
  return request({
    url: `/notifications/${id}/read`,
    method: 'put'
  })
}

/**
 * 全部标记已读
 */
export function markAllNotificationsRead() {
  return request({
    url: '/notifications/read-all',
    method: 'put'
  })
}
