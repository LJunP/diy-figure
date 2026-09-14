<template>
  <div class="default-layout">
    <AmbientBackground :particles="false" />

    <!-- 顶部导航栏 -->
    <header class="nav-header" :class="{ scrolled: isScrolled }">
      <div class="nav-container">
        <div class="nav-left">
          <router-link to="/" class="logo">
            <span class="logo-icon">DIY</span>
            <span class="logo-text">FIGURE</span>
            <span class="logo-dot"></span>
          </router-link>
        </div>

        <nav class="nav-center">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link v-if="userStore.isLoggedIn" to="/series" class="nav-link">我的系列</router-link>
          <router-link v-if="userStore.isLoggedIn" to="/orders" class="nav-link">我的订单</router-link>
          <router-link v-if="userStore.isLoggedIn" to="/profile" class="nav-link">个人中心</router-link>
        </nav>

        <div class="nav-right">
          <template v-if="userStore.isLoggedIn">
            <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99" class="notice-badge">
              <el-button text class="notice-btn" @click="$router.push('/notifications')">
                <el-icon :size="18"><Bell /></el-icon>
              </el-button>
            </el-badge>
            <el-button v-if="userStore.isAdmin" text class="admin-btn" @click="$router.push('/admin')">
              <el-icon class="el-icon--left"><Operation /></el-icon>
              运营后台
            </el-button>
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <span class="user-avatar">{{ userStore.username.charAt(0).toUpperCase() }}</span>
                <span class="user-name">{{ userStore.username }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="notifications">
                    消息中心
                    <span v-if="unreadCount" class="dd-badge">{{ unreadCount }}</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="series">我的系列</el-dropdown-item>
                  <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>运营后台</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text class="login-btn" @click="$router.push('/login')">登录</el-button>
            <el-button round class="register-btn" @click="$router.push('/login')">
              开始体验
              <el-icon class="el-icon--right"><Right /></el-icon>
            </el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 页脚 -->
    <footer class="site-footer">
      <div class="footer-glow"></div>
      <div class="footer-container">
        <div class="footer-brand">
          <span class="logo-icon">DIY</span>
          <span class="logo-text">FIGURE</span>
          <p class="footer-desc">AI 对话式手办盲盒定制平台</p>
        </div>
        <div class="footer-links">
          <div class="footer-col">
            <h4>产品</h4>
            <router-link to="/">功能介绍</router-link>
            <router-link to="/">规格档位</router-link>
            <router-link to="/">定制流程</router-link>
          </div>
          <div class="footer-col">
            <h4>帮助</h4>
            <router-link to="/login">登录注册</router-link>
            <router-link to="/profile">账户管理</router-link>
            <router-link to="/series">创建系列</router-link>
            <router-link to="/terms">用户协议与合规说明</router-link>
          </div>
          <div class="footer-col">
            <h4>关于</h4>
            <span>DIY Figure Demo</span>
            <span>AI + 手办盲盒</span>
            <span>2026</span>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <p>© 2026 DIY Figure — AI 对话式手办盲盒定制平台 · Demo Version</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Right, Operation, Bell } from '@element-plus/icons-vue'
import AmbientBackground from '@/components/AmbientBackground.vue'
import { getUnreadCount } from '@/api/notification'

const userStore = useUserStore()
const router = useRouter()
const isScrolled = ref(false)
const unreadCount = ref(0)

function handleScroll() {
  isScrolled.value = window.scrollY > 12
}

// 未读数:登录状态变化时拉取,并在每次路由跳转后刷新(用户读完后角标要跟着消)
async function refreshUnread() {
  if (!userStore.isLoggedIn) {
    unreadCount.value = 0
    return
  }
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data?.unread ?? 0
  } catch (e) {
    // 通知接口异常不影响导航
  }
}

watch(() => userStore.isLoggedIn, refreshUnread)
router.afterEach(() => refreshUnread())

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  handleScroll()
  refreshUnread()
})
onUnmounted(() => window.removeEventListener('scroll', handleScroll))

function handleCommand(command) {
  switch (command) {
    case 'notifications':
      router.push('/notifications')
      break
    case 'profile':
      router.push('/profile')
      break
    case 'series':
      router.push('/series')
      break
    case 'orders':
      router.push('/orders')
      break
    case 'admin':
      router.push('/admin')
      break
    case 'logout':
      userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/')
      break
  }
}
</script>

<style scoped>
.default-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
}

/* ===== 导航栏 ===== */
.nav-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: rgba(7, 7, 14, 0.6);
  backdrop-filter: blur(28px) saturate(160%);
  -webkit-backdrop-filter: blur(28px) saturate(160%);
  border-bottom: 1px solid transparent;
  transition: all 0.4s var(--ease-out);
}

.nav-header.scrolled {
  background: rgba(7, 7, 14, 0.82);
  border-bottom-color: rgba(255, 255, 255, 0.07);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);
}

.nav-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 32px;
  height: 66px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
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

.logo-text {
  color: #fff;
  font-weight: 300;
  letter-spacing: 2px;
}

.logo-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--brand-3, #ec4899);
  margin-left: 2px;
  box-shadow: 0 0 10px rgba(236, 72, 153, 0.8);
  animation: pulse 2.5s ease-in-out infinite;
}

.nav-center {
  display: flex;
  gap: 32px;
}

.nav-link {
  font-size: 14px;
  color: var(--text-3);
  transition: color 0.25s ease;
  position: relative;
  padding: 4px 0;
}

.nav-link::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 50%;
  right: 50%;
  height: 2px;
  background: var(--gradient-brand);
  border-radius: 1px;
  transition: all 0.3s var(--ease-out);
}

.nav-link:hover,
.nav-link.router-link-active {
  color: #fff;
}

.nav-link.router-link-active::after,
.nav-link:hover::after {
  left: 0;
  right: 0;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-btn {
  color: var(--brand-1, #6d7cff) !important;
}

.notice-badge { display: flex; align-items: center; }
.notice-btn { padding: 8px !important; color: var(--text-2) !important; }
.notice-btn:hover { color: #fff !important; }

.dd-badge {
  margin-left: 6px; min-width: 18px; padding: 0 5px;
  display: inline-block; text-align: center; line-height: 18px;
  border-radius: 9px; font-size: 11px;
  background: var(--el-color-danger); color: #fff;
}

.login-btn {
  color: var(--text-2) !important;
}

.register-btn {
  background: var(--gradient-brand) !important;
  border: none !important;
  color: #fff !important;
  box-shadow: 0 2px 16px rgba(109, 124, 255, 0.3);
  transition: all 0.3s var(--ease-out) !important;
}

.register-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 24px rgba(109, 124, 255, 0.5) !important;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--text-2);
  padding: 4px 8px;
  border-radius: 10px;
  transition: background 0.25s ease;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.05);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--gradient-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  box-shadow: 0 0 12px rgba(109, 124, 255, 0.4);
}

.user-name {
  font-size: 14px;
}

/* ===== 主内容 ===== */
.main-content {
  flex: 1;
  padding-top: 66px;
  position: relative;
  z-index: 1;
}

/* ===== 页脚 ===== */
.site-footer {
  position: relative;
  background: rgba(5, 5, 10, 0.7);
  backdrop-filter: blur(20px);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding: 56px 32px 24px;
  overflow: hidden;
}

.footer-glow {
  position: absolute;
  top: -40%;
  left: 50%;
  transform: translateX(-50%);
  width: 70%;
  height: 100%;
  background: radial-gradient(ellipse at center, rgba(109, 124, 255, 0.08), transparent 70%);
  pointer-events: none;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  gap: 48px;
  position: relative;
}

.footer-brand .logo {
  font-size: 22px;
}

.footer-desc {
  color: var(--text-4);
  font-size: 13px;
  margin-top: 12px;
}

.footer-links {
  display: flex;
  gap: 80px;
}

.footer-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.footer-col h4 {
  color: var(--text-3);
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
}

.footer-col a,
.footer-col span {
  color: var(--text-4);
  font-size: 13px;
  transition: color 0.2s;
}

.footer-col a:hover {
  color: var(--brand-1, #6d7cff);
}

.footer-bottom {
  max-width: 1200px;
  margin: 32px auto 0;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.04);
  text-align: center;
}

.footer-bottom p {
  color: var(--text-4);
  font-size: 12px;
}
</style>
