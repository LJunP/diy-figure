/**
 * 用户状态管理(Pinia)
 * 管理 token、用户信息、登录/退出
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // ===== State =====
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  // ===== Getters =====
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const username = computed(() => userInfo.value?.username || '')

  // ===== Actions =====

  /**
   * 登录
   */
  async function login(loginData) {
    const res = await loginApi(loginData)
    token.value = res.data.token
    userInfo.value = res.data.user
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userInfo', JSON.stringify(res.data.user))
    return res
  }

  /**
   * 退出登录
   */
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  /**
   * 刷新用户信息(从后端拉取最新)
   */
  async function refreshUserInfo() {
    try {
      const res = await getUserInfo()
      userInfo.value = res.data
      localStorage.setItem('userInfo', JSON.stringify(res.data))
    } catch (e) {
      // 获取失败(token 过期等),清除本地状态
      logout()
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    username,
    login,
    logout,
    refreshUserInfo
  }
})
