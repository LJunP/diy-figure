<template>
  <div class="auth-aux">
    <div class="auth-card glass-strong">
      <h1>重置密码</h1>
      <el-form v-if="status !== 'ok'" @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="password" type="password" show-password placeholder="新密码(至少 6 位)" size="large" />
        </el-form-item>
        <el-button class="submit-btn" :loading="loading" @click="submit">确认重置</el-button>
      </el-form>
      <p v-else>密码已更新,请使用新密码登录。</p>
      <el-button v-if="status === 'ok'" type="primary" round @click="$router.push('/login')">去登录</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { resetPassword } from '@/api/auth'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const password = ref('')
const loading = ref(false)
const status = ref('form')

async function submit() {
  if (!route.query.token) {
    ElMessage.error('缺少重置令牌')
    return
  }
  if (!password.value || password.value.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  loading.value = true
  try {
    await resetPassword({ token: route.query.token, newPassword: password.value })
    status.value = 'ok'
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-aux { min-height: 70vh; display: flex; align-items: center; justify-content: center; padding: 32px; }
.auth-card { width: 420px; padding: 32px; display: flex; flex-direction: column; gap: 16px; }
h1 { color: #fff; font-size: 22px; text-align: center; }
p { color: var(--text-2); text-align: center; }
.submit-btn { width: 100%; }
</style>
