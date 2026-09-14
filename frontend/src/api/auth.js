/**
 * 认证相关 API
 */
import request from './request'

/**
 * 用户注册
 * @param {Object} data - { username, password, email }
 */
export function register(data) {
  return request.post('/auth/register', data)
}

/**
 * 用户登录
 * @param {Object} data - { username, password } 或 { email, password }
 * @returns {Promise} { token, user: { id, username, role } }
 */
export function login(data) {
  return request.post('/auth/login', data)
}

/**
 * 获取当前用户信息
 */
export function getUserInfo() {
  return request.get('/auth/me')
}

/**
 * 修改密码
 * @param {Object} data - { oldPassword, newPassword }
 */
export function updatePassword(data) {
  return request.put('/auth/password', data)
}

export function verifyEmail(token) {
  return request.post('/auth/verify-email', { token })
}

export function resendVerification(email) {
  return request.post('/auth/resend-verification', { email })
}

export function forgotPassword(email) {
  return request.post('/auth/forgot-password', { email })
}

export function resetPassword(data) {
  return request.post('/auth/reset-password', data)
}
