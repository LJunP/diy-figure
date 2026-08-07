<template>
  <div class="login-page">
    <!-- 背景效果 -->
    <div class="login-bg">
      <div class="bg-gradient"></div>
      <div class="bg-particles"></div>
    </div>

    <div class="login-container">
      <!-- 左侧品牌区 -->
      <div class="brand-side">
        <div class="brand-content">
          <div class="brand-logo">
            <span class="logo-icon">DIY</span>
            <span class="logo-text">FIGURE</span>
          </div>
          <h1 class="brand-title">AI 对话式<br />手办盲盒定制平台</h1>
          <p class="brand-desc">
            与 GPT-4o 协作设计原创角色,一键生成概念图与 3D 参考模型,
            经人工终审后量产实体盲盒。
          </p>
          <div class="brand-features">
            <div class="brand-feature">
              <el-icon :size="20"><ChatDotRound /></el-icon>
              <span>AI 流式对话设计</span>
            </div>
            <div class="brand-feature">
              <el-icon :size="20"><Picture /></el-icon>
              <span>DALL-E 3 概念图生成</span>
            </div>
            <div class="brand-feature">
              <el-icon :size="20"><View /></el-icon>
              <span>Meshy AI 3D 模型</span>
            </div>
            <div class="brand-feature">
              <el-icon :size="20"><Box /></el-icon>
              <span>全流程状态追踪</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-side">
        <div class="form-card glass">
          <div class="form-header">
            <h2>{{ activeTab === 'login' ? '欢迎回来' : '创建账号' }}</h2>
            <p>{{ activeTab === 'login' ? '登录开始你的设计之旅' : '注册即可免费体验 AI 手办设计' }}</p>
          </div>

          <el-tabs v-model="activeTab" stretch class="login-tabs">
            <!-- 登录 -->
            <el-tab-pane label="登录" name="login">
              <el-form
                ref="loginFormRef"
                :model="loginForm"
                :rules="loginRules"
                label-position="top"
                @submit.prevent="handleLogin"
              >
                <el-form-item label="用户名 / 邮箱" prop="account">
                  <el-input
                    v-model="loginForm.account"
                    placeholder="请输入用户名或邮箱"
                    :prefix-icon="User"
                    size="large"
                  />
                </el-form-item>
                <el-form-item label="密码" prop="password">
                  <el-input
                    v-model="loginForm.password"
                    type="password"
                    placeholder="请输入密码"
                    :prefix-icon="Lock"
                    show-password
                    size="large"
                    @keyup.enter="handleLogin"
                  />
                </el-form-item>
                <el-button
                  type="primary"
                  class="submit-btn"
                  :loading="loading"
                  @click="handleLogin"
                >
                  登录
                </el-button>
              </el-form>
            </el-tab-pane>

            <!-- 注册 -->
            <el-tab-pane label="注册" name="register">
              <el-form
                ref="registerFormRef"
                :model="registerForm"
                :rules="registerRules"
                label-position="top"
                @submit.prevent="handleRegister"
              >
                <el-form-item label="用户名" prop="username">
                  <el-input
                    v-model="registerForm.username"
                    placeholder="请输入用户名"
                    :prefix-icon="User"
                    size="large"
                  />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input
                    v-model="registerForm.email"
                    placeholder="请输入邮箱"
                    :prefix-icon="Message"
                    size="large"
                  />
                </el-form-item>
                <el-form-item label="密码" prop="password">
                  <el-input
                    v-model="registerForm.password"
                    type="password"
                    placeholder="请输入密码(至少 6 位)"
                    :prefix-icon="Lock"
                    show-password
                    size="large"
                  />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input
                    v-model="registerForm.confirmPassword"
                    type="password"
                    placeholder="请再次输入密码"
                    :prefix-icon="Lock"
                    show-password
                    size="large"
                    @keyup.enter="handleRegister"
                  />
                </el-form-item>
                <el-button
                  type="primary"
                  class="submit-btn"
                  :loading="loading"
                  @click="handleRegister"
                >
                  注册
                </el-button>
              </el-form>
            </el-tab-pane>
          </el-tabs>

          <!-- Demo 账号提示 -->
          <div class="demo-accounts">
            <div class="demo-title">Demo 体验账号</div>
            <div class="demo-row" @click="fillAccount('orderuser', 'order123')">
              <span class="demo-label">用户</span>
              <span class="demo-cred">orderuser / order123</span>
              <el-icon class="demo-copy"><CopyDocument /></el-icon>
            </div>
            <div class="demo-row" @click="fillAccount('admin', 'admin123')">
              <span class="demo-label">管理员</span>
              <span class="demo-cred">admin / admin123</span>
              <el-icon class="demo-copy"><CopyDocument /></el-icon>
            </div>
            <div class="demo-row" @click="fillAccount('testuser1', '123456')">
              <span class="demo-label">用户</span>
              <span class="demo-cred">testuser1 / 123456</span>
              <el-icon class="demo-copy"><CopyDocument /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, ChatDotRound, Picture, View, Box, CopyDocument } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()

const loginForm = reactive({ account: '', password: '' })
const loginRules = {
  account: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerForm = reactive({ username: '', email: '', password: '', confirmPassword: '' })
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度 2-20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function fillAccount(account, password) {
  loginForm.account = account
  loginForm.password = password
  activeTab.value = 'login'
  ElMessage.success('账号已填入,点击登录即可')
}

async function handleLogin() {
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login({ account: loginForm.account, password: loginForm.password })
      ElMessage.success('登录成功')
      const redirect = route.query.redirect || '/'
      router.push(redirect)
    } catch (e) {
      // 错误已由 axios 拦截器处理
    } finally {
      loading.value = false
    }
  })
}

async function handleRegister() {
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await register({
        username: registerForm.username,
        email: registerForm.email,
        password: registerForm.password
      })
      ElMessage.success('注册成功,请登录')
      activeTab.value = 'login'
      loginForm.account = registerForm.username
    } catch (e) {
      // 错误已由 axios 拦截器处理
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.bg-gradient {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 20% 30%, rgba(102, 126, 234, 0.2) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 70%, rgba(118, 75, 162, 0.2) 0%, transparent 50%);
}

.bg-particles {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(2px 2px at 20% 30%, rgba(255,255,255,0.15), transparent),
    radial-gradient(2px 2px at 60% 70%, rgba(255,255,255,0.1), transparent),
    radial-gradient(1px 1px at 50% 50%, rgba(255,255,255,0.1), transparent),
    radial-gradient(1px 1px at 80% 10%, rgba(255,255,255,0.1), transparent);
  background-size: 200% 200%;
  animation: float 20s ease-in-out infinite;
}

.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  width: 900px;
  max-width: 90vw;
  min-height: 560px;
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(20, 20, 20, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

/* 左侧品牌区 */
.brand-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background:
    linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
}

.brand-content {
  max-width: 360px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 32px;
}

.logo-icon { color: #667eea; font-weight: 900; }
.logo-text { color: #fff; font-weight: 300; letter-spacing: 2px; }

.brand-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  line-height: 1.3;
  margin-bottom: 16px;
}

.brand-desc {
  font-size: 14px;
  color: #999;
  line-height: 1.7;
  margin-bottom: 32px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.brand-feature {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #ccc;
  font-size: 14px;
}

.brand-feature .el-icon {
  color: #667eea;
}

/* 右侧表单区 */
.form-side {
  width: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-card {
  width: 100%;
}

.form-header {
  margin-bottom: 24px;
}

.form-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 4px;
}

.form-header p {
  font-size: 14px;
  color: #888;
}

.login-tabs {
  margin-bottom: 20px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  margin-top: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

/* Demo 账号 */
.demo-accounts {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.demo-title {
  font-size: 12px;
  color: #666;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.demo-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 4px;
}

.demo-row:hover {
  background: rgba(255, 255, 255, 0.05);
}

.demo-label {
  font-size: 12px;
  color: #667eea;
  min-width: 36px;
}

.demo-cred {
  font-size: 13px;
  color: #ccc;
  flex: 1;
  font-family: 'SF Mono', 'Monaco', monospace;
}

.demo-copy {
  color: #555;
  font-size: 14px;
}

/* 响应式 */
@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
    width: 92vw;
    min-height: auto;
  }
  .brand-side {
    padding: 32px 24px;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  }
  .form-side {
    width: 100%;
    padding: 24px;
  }
}
</style>
