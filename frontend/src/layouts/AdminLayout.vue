<template>
  <div class="admin-layout">
    <AmbientBackground :particles="false" />

    <!-- 侧边栏 -->
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <span class="brand-icon">DIY</span>
        <div class="brand-text">
          <span class="brand-name">FIGURE</span>
          <span class="brand-sub">运营后台</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/admin" class="sidebar-item" :class="{ active: $route.path === '/admin' }">
          <el-icon :size="20"><Odometer /></el-icon>
          <span>控制台</span>
        </router-link>
        <router-link to="/admin/reviews" class="sidebar-item">
          <el-icon :size="20"><DocumentChecked /></el-icon>
          <span>待终审</span>
          <el-badge v-if="pendingCount > 0" :value="pendingCount" class="sidebar-badge" />
        </router-link>
        <router-link to="/admin/quotes" class="sidebar-item">
          <el-icon :size="20"><Money /></el-icon>
          <span>待报价</span>
          <el-badge v-if="quoteCount > 0" :value="quoteCount" class="sidebar-badge" />
        </router-link>
        <router-link to="/admin/production" class="sidebar-item">
          <el-icon :size="20"><Setting /></el-icon>
          <span>生产物流</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <router-link to="/" class="back-link">
          <el-icon :size="16"><Back /></el-icon>
          <span>返回前台</span>
        </router-link>
      </div>
    </aside>

    <!-- 主内容 -->
    <div class="admin-main">
      <header class="admin-header">
        <h1 class="page-title">{{ $route.meta.title || '运营后台' }}</h1>
        <div class="header-right">
          <span class="admin-user">
            <span class="admin-avatar">{{ userStore.username.charAt(0).toUpperCase() }}</span>
            <span>{{ userStore.username }}</span>
            <el-tag size="small" type="danger" effect="dark">ADMIN</el-tag>
          </span>
          <el-button text @click="handleLogout">退出</el-button>
        </div>
      </header>

      <main class="admin-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getDashboardStats } from '@/api/admin'
import { Odometer, DocumentChecked, Money, Setting, Back } from '@element-plus/icons-vue'
import AmbientBackground from '@/components/AmbientBackground.vue'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
function handleLogout() {
  userStore.logout()
  router.push('/login')
}
const pendingCount = ref(0)
const quoteCount = ref(0)

async function refreshBadges() {
  try {
    const res = await getDashboardStats()
    pendingCount.value = res.data?.pendingReviews || 0
    quoteCount.value = res.data?.pendingQuotes || 0
  } catch (e) {
    // ignore
  }
}

onMounted(refreshBadges)

// 路由变化时刷新 badge
watch(() => route.fullPath, () => {
  refreshBadges()
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  position: relative;
  z-index: 1;
}

/* 侧边栏 */
.admin-sidebar {
  width: 232px;
  background: rgba(5, 5, 10, 0.7);
  backdrop-filter: blur(28px) saturate(160%);
  -webkit-backdrop-filter: blur(28px) saturate(160%);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 100;
}

.sidebar-brand {
  padding: 22px 24px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.brand-icon {
  font-size: 18px;
  font-weight: 900;
  background: var(--gradient-brand);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: gradientShift 6s ease infinite;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-name {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 1px;
}

.brand-sub {
  font-size: 11px;
  color: var(--text-4);
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  color: var(--text-3);
  font-size: 14px;
  transition: all 0.25s var(--ease-out);
  position: relative;
  overflow: hidden;
}

.sidebar-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%) scaleY(0);
  width: 3px;
  height: 22px;
  background: var(--gradient-brand);
  border-radius: 0 2px 2px 0;
  transition: transform 0.3s var(--ease-out);
}

.sidebar-item:hover {
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-1);
}

.sidebar-item.active {
  background: linear-gradient(90deg, rgba(109, 124, 255, 0.14), rgba(109, 124, 255, 0.02));
  color: var(--brand-1);
}

.sidebar-item.active::before {
  transform: translateY(-50%) scaleY(1);
}

.sidebar-badge :deep(.el-badge__content) {
  background: var(--el-color-danger);
  border: none;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.back-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  color: var(--text-4);
  font-size: 13px;
  border-radius: 10px;
  transition: all 0.25s var(--ease-out);
}

.back-link:hover {
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-1);
}

/* 主内容 */
.admin-main {
  flex: 1;
  margin-left: 232px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.admin-header {
  height: 66px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(7, 7, 14, 0.7);
  backdrop-filter: blur(24px) saturate(160%);
  -webkit-backdrop-filter: blur(24px) saturate(160%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  position: sticky;
  top: 0;
  z-index: 50;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  background: var(--gradient-brand);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: gradientShift 6s ease infinite;
}

.header-right {
  display: flex;
  align-items: center;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-2);
  font-size: 14px;
  padding: 4px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
}

.admin-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f87171 0%, #fbbf24 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  box-shadow: 0 0 12px rgba(248, 113, 113, 0.4);
}

.admin-content {
  flex: 1;
  padding: 28px 32px;
  overflow-y: auto;
}
</style>
