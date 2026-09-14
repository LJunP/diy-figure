<template>
  <div class="login-page">
    <AmbientBackground />

    <div class="login-container glass-strong" :class="{ loading }">
      <!-- 渐变描边光 -->
      <div class="border-glow"></div>

      <!-- 左侧品牌区 -->
      <div class="brand-side">
        <div class="brand-content">
          <div class="brand-logo">
            <span class="logo-icon">DIY</span>
            <span class="logo-text">FIGURE</span>
            <span class="logo-dot"></span>
          </div>
          <h1 class="brand-title">AI 对话式<br />手办盲盒定制平台</h1>
          <p class="brand-desc">
            与 GPT-4o 协作设计原创角色,一键生成概念图与 3D 参考模型,
            经人工终审后量产实体盲盒。
          </p>
          <div class="brand-features">
            <div class="brand-feature" v-for="(f, i) in brandFeatures" :key="i">
              <el-icon :size="20"><component :is="f.icon" /></el-icon>
              <span>{{ f.text }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-side">
        <div class="form-card">
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
                  class="submit-btn"
                  :loading="loading"
                  @click="handleLogin"
                >
                  登录
                </el-button>
                <el-button text class="aux-link" @click="showForgot = true">忘记密码?</el-button>
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
                  class="submit-btn"
                  :loading="loading"
                  @click="handleRegister"
                >
                  注册
                </el-button>
                <p class="terms-hint">
                  注册即表示你已阅读并同意
                  <router-link to="/terms">用户协议与合规说明</router-link>
                </p>
              </el-form>
            </el-tab-pane>
          </el-tabs>

          <el-dialog v-model="showForgot" title="重置密码" width="400px">
            <el-input v-model="forgotEmail" placeholder="注册邮箱" size="large" />
            <template #footer>
              <el-button @click="showForgot = false">取消</el-button>
              <el-button type="primary" :loading="forgotLoading" @click="handleForgot">发送重置邮件</el-button>
            </template>
          </el-dialog>

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
import { register, forgotPassword } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, ChatDotRound, Picture, View, Box, CopyDocument } from '@element-plus/icons-vue'
import AmbientBackground from '@/components/AmbientBackground.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)
const showForgot = ref(false)
const forgotEmail = ref('')
const forgotLoading = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()

const brandFeatures = [
  { icon: ChatDotRound, text: 'AI 流式对话设计' },
  { icon: Picture, text: 'DALL-E 3 概念图生成' },
  { icon: View, text: 'Meshy AI 3D 模型' },
  { icon: Box, text: '全流程状态追踪' }
]

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

async function handleForgot() {
  if (!forgotEmail.value) {
    ElMessage.warning('请输入邮箱')
    return
  }
  forgotLoading.value = true
  try {
    await forgotPassword(forgotEmail.value)
    ElMessage.success('如果该邮箱已注册,你会收到重置邮件(未配置 SMTP 时请查看后端日志)')
    showForgot.value = false
  } catch {
    /* 拦截器已提示 */
  } finally {
    forgotLoading.value = false
  }
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
  z-index: 1;
}

.login-container {
  position: relative;
  display: flex;
  width: 920px;
  max-width: 90vw;
  min-height: 580px;
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.55), var(--glass-highlight);
  animation: fadeInUp 0.8s var(--ease-out);
}

/* 描边光 */
.border-glow {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  padding: 1px;
  background: linear-gradient(135deg, rgba(109, 124, 255, 0.6), rgba(168, 85, 247, 0.3) 50%, rgba(236, 72, 153, 0.4));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
  z-index: 2;
}

/* 左侧品牌区 */
.brand-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background:
    radial-gradient(ellipse at 30% 20%, rgba(109, 124, 255, 0.18), transparent 60%),
    radial-gradient(ellipse at 70% 80%, rgba(236, 72, 153, 0.12), transparent 60%),
    linear-gradient(135deg, rgba(109, 124, 255, 0.06) 0%, rgba(168, 85, 247, 0.06) 100%);
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

.logo-icon {
  background: var(--gradient-brand);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 900;
  animation: gradientShift 6s ease infinite;
}
.logo-text { color: #fff; font-weight: 300; letter-spacing: 2px; }
.logo-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--brand-3, #ec4899);
  box-shadow: 0 0 10px rgba(236, 72, 153, 0.8);
  margin-left: 2px;
  animation: pulse 2.5s ease-in-out infinite;
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  line-height: 1.3;
  margin-bottom: 16px;
}

.brand-desc {
  font-size: 14px;
  color: var(--text-3);
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
  color: var(--text-2);
  font-size: 14px;
  padding: 8px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
  transition: all 0.3s var(--ease-out);
}

.brand-feature:hover {
  background: rgba(109, 124, 255, 0.1);
  transform: translateX(4px);
}

.brand-feature .el-icon {
  color: var(--brand-1);
}

/* 右侧表单区 */
.form-side {
  width: 440px;
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
  color: var(--text-3);
}

.login-tabs {
  margin-bottom: 20px;
}

.aux-link { margin-top: 8px; width: 100%; color: var(--text-3); }
.terms-hint { margin-top: 12px; font-size: 12px; color: var(--text-4); text-align: center; }
.terms-hint a { color: var(--brand-1); }
.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  margin-top: 8px;
  background: var(--gradient-brand) !important;
  border: none !important;
  color: #fff !important;
  box-shadow: 0 4px 20px rgba(109, 124, 255, 0.35);
  position: relative;
  overflow: hidden;
  transition: transform 0.3s var(--ease-spring) !important;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(109, 124, 255, 0.5) !important;
}

.submit-btn::after {
  content: '';
  position: absolute;
  top: 0; left: -80%;
  width: 50%; height: 100%;
  background: linear-gradient(100deg, transparent 20%, rgba(255,255,255,0.35) 50%, transparent 80%);
  transform: skewX(-20deg);
  transition: left 0.7s var(--ease-out);
}
.submit-btn:hover::after { left: 130%; }

/* Demo 账号 */
.demo-accounts {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.demo-title {
  font-size: 12px;
  color: var(--text-4);
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.demo-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s var(--ease-out);
  margin-bottom: 4px;
}

.demo-row:hover {
  background: rgba(109, 124, 255, 0.08);
  transform: translateX(2px);
}

.demo-label {
  font-size: 12px;
  color: var(--brand-1);
  min-width: 36px;
}

.demo-cred {
  font-size: 13px;
  color: var(--text-2);
  flex: 1;
  font-family: 'SF Mono', 'Monaco', monospace;
}

.demo-copy {
  color: var(--text-4);
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
