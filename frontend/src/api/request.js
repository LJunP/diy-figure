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

// ===== 请求拦截器:自动附带 JWT token =====
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// ===== 响应拦截器:统一错误处理 =====
request.interceptors.response.use(
  (response) => {
    const res = response.data

    // 后端统一返回 { code, message, data, timestamp }
    if (res.code === 200) {
      return res
    }

    // 业务错误:显示错误提示
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response

      if (status === 401) {
        // token 过期或未登录,清除本地存储并跳转登录页
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        ElMessage.warning('登录已过期,请重新登录')
        router.push('/login')
      } else {
        const message = data?.message || `请求失败(${status})`
        ElMessage.error(message)
      }
    } else if (error.message.includes('timeout')) {
      ElMessage.error('请求超时,请稍后重试')
    } else {
      ElMessage.error('网络异常,请检查网络连接')
    }

    return Promise.reject(error)
  }
)

export default request
