/**
 * Axios 请求实例
 * 统一配置请求拦截器(token 自动附带)和响应拦截器(401 自动跳转登录)
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

export function handleUnauthorized() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  import('@/stores/user').then(({ useUserStore }) => {
    try {
      useUserStore().logout()
    } catch (_) {
      /* Pinia 尚未就绪时忽略 */
    }
  })
  const current = router.currentRoute.value
  if (current?.name === 'Login') {
    return
  }
  const redirect = current?.fullPath
  router.push({
    name: 'Login',
    query: redirect && redirect !== '/login' ? { redirect } : {}
  })
}

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        ElMessage.warning('登录已过期,请重新登录')
        handleUnauthorized()
      } else {
        const message = data?.message || `请求失败(${status})`
        ElMessage.error(message)
      }
    } else if (error.message?.includes('timeout')) {
      ElMessage.error('请求超时,请稍后重试')
    } else {
      ElMessage.error('网络异常,请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request
