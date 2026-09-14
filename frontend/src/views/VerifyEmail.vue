<template>
  <div class="auth-aux">
    <div class="auth-card glass-strong">
      <h1>验证邮箱</h1>
      <p v-if="status === 'working'">正在验证…</p>
      <p v-else-if="status === 'ok'">邮箱已验证,可以继续使用全部功能。</p>
      <p v-else>验证链接无效或已过期。你可以登录后在个人中心重新发送。</p>
      <el-button type="primary" round @click="$router.push(status === 'ok' ? '/' : '/login')">
        {{ status === 'ok' ? '进入平台' : '去登录' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { verifyEmail } from '@/api/auth'

const route = useRoute()
const status = ref('working')

onMounted(async () => {
  const token = route.query.token
  if (!token) {
    status.value = 'fail'
    return
  }
  try {
    await verifyEmail(token)
    status.value = 'ok'
  } catch {
    status.value = 'fail'
  }
})
</script>

<style scoped>
.auth-aux { min-height: 70vh; display: flex; align-items: center; justify-content: center; padding: 32px; }
.auth-card { width: 420px; padding: 32px; text-align: center; display: flex; flex-direction: column; gap: 16px; }
h1 { color: #fff; font-size: 22px; }
p { color: var(--text-2); }
</style>
