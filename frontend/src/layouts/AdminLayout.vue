<template>
  <div class="admin-layout">
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
          <el-icon :size="20"><Dashboard /></el-icon>
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
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getDashboardStats } from '@/api/admin'
import { Dashboard, DocumentChecked, Money, Setting, Back } from '@element-plus/icons-vue'

const userStore = useUserStore()
const route = useRoute()
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
  background: #0d0d0d;
}

/* 侧边栏 */
.admin-sidebar {
  width: 220px;
  background: #080808;
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
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.brand-icon {
  font-size: 18px;
  font-weight: 900;
  color: #667eea;
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
  color: #666;
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
  border-radius: 10px;
  color: #888;
  font-size: 14px;
  transition: all 0.2s;
  position: relative;
}

.sidebar-item:hover {
  background: rgba(255, 255, 255, 0.04);
  color: #ccc;
}

.sidebar-item.active {
  background: rgba(102, 126, 234, 0.12);
  color: #667eea;
}

.sidebar-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: #667eea;
  border-radius: 0 2px 2px 0;
}

.sidebar-badge :deep(.el-badge__content) {
  background: #f56c6c;
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
  color: #666;
  font-size: 13px;
  border-radius: 8px;
  transition: all 0.2s;
}

.back-link:hover {
  background: rgba(255, 255, 255, 0.04);
  color: #ccc;
}

/* 主内容 */
.admin-main {
  flex: 1;
  margin-left: 220px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.admin-header {
  height: 64px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(13, 13, 13, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  position: sticky;
  top: 0;
  z-index: 50;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.header-right {
  display: flex;
  align-items: center;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ccc;
  font-size: 14px;
}

.admin-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f56c6c 0%, #e6a23c 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
}

.admin-content {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto;
}
</style>
