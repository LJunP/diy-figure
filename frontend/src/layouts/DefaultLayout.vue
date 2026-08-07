<template>
  <div class="default-layout">
    <!-- 顶部导航栏 -->
    <header class="nav-header">
      <div class="nav-container">
        <div class="nav-left">
          <router-link to="/" class="logo">
            <span class="logo-icon">DIY</span>
            <span class="logo-text">FIGURE</span>
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
            <el-button v-if="userStore.isAdmin" text class="admin-btn" @click="$router.push('/admin')">
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
            <el-button type="primary" round class="register-btn" @click="$router.push('/login')">
              开始体验
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
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()

function handleCommand(command) {
  switch (command) {
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
  background: #0d0d0d;
}

/* ===== 导航栏 ===== */
.nav-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: rgba(13, 13, 13, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  transition: all 0.3s ease;
}

.nav-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 32px;
  height: 64px;
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
  color: #667eea;
  font-weight: 900;
}

.logo-text {
  color: #fff;
  font-weight: 300;
  letter-spacing: 2px;
}

.nav-center {
  display: flex;
  gap: 32px;
}

.nav-link {
  font-size: 14px;
  color: #999;
  transition: color 0.2s;
  position: relative;
}

.nav-link:hover,
.nav-link.router-link-active {
  color: #fff;
}

.nav-link.router-link-active::after {
  content: '';
  position: absolute;
  bottom: -22px;
  left: 0;
  right: 0;
  height: 2px;
  background: #667eea;
  border-radius: 1px;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-btn {
  color: #667eea !important;
}

.login-btn {
  color: #ccc !important;
}

.register-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #ccc;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
}

.user-name {
  font-size: 14px;
}

/* ===== 主内容 ===== */
.main-content {
  flex: 1;
  padding-top: 64px;
}

/* ===== 页脚 ===== */
.site-footer {
  background: #080808;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding: 48px 32px 24px;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  gap: 48px;
}

.footer-brand .logo {
  font-size: 22px;
}

.footer-desc {
  color: #666;
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
  color: #999;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
}

.footer-col a,
.footer-col span {
  color: #555;
  font-size: 13px;
  transition: color 0.2s;
}

.footer-col a:hover {
  color: #667eea;
}

.footer-bottom {
  max-width: 1200px;
  margin: 32px auto 0;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.04);
  text-align: center;
}

.footer-bottom p {
  color: #444;
  font-size: 12px;
}
</style>
