<template>
  <div class="admin-dashboard">
    <!-- 欢迎条 -->
    <div class="welcome-bar glass">
      <div>
        <h2>欢迎回来,{{ userStore.username }}</h2>
        <p>当前共有 <strong>{{ totalPending }}</strong> 个待处理任务</p>
      </div>
      <div class="welcome-time">
        <el-icon><Calendar /></el-icon>
        <span>{{ currentTime }}</span>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div v-for="card in statCards" :key="card.key" class="stat-card glass" @click="card.action?.()">
        <div class="stat-icon" :style="{ background: card.bg, color: card.color }">
          <el-icon :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value">{{ stats[card.key] || 0 }}</div>
        </div>
        <el-icon v-if="card.action" class="stat-arrow"><ArrowRight /></el-icon>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="section-header">
      <span class="section-label">QUICK ACCESS</span>
      <h3 class="section-title">快捷入口</h3>
    </div>
    <div class="quick-grid">
      <div class="quick-card glass" @click="$router.push('/admin/reviews')">
        <div class="quick-icon" style="background: rgba(102, 126, 234, 0.15); color: #667eea">
          <el-icon :size="28"><DocumentChecked /></el-icon>
        </div>
        <div class="quick-info">
          <h4>待终审管理</h4>
          <p>审核用户提交的角色设计</p>
        </div>
      </div>
      <div class="quick-card glass" @click="$router.push('/admin/quotes')">
        <div class="quick-icon" style="background: rgba(245, 87, 108, 0.15); color: #f5576c">
          <el-icon :size="28"><Money /></el-icon>
        </div>
        <div class="quick-info">
          <h4>待报价管理</h4>
          <p>为终审通过的订单定价</p>
        </div>
      </div>
      <div class="quick-card glass" @click="$router.push('/admin/production')">
        <div class="quick-icon" style="background: rgba(67, 233, 123, 0.15); color: #43e97b">
          <el-icon :size="28"><Setting /></el-icon>
        </div>
        <div class="quick-info">
          <h4>生产物流管理</h4>
          <p>生产、质检、发货全流程</p>
        </div>
      </div>
    </div>

    <!-- 订单状态分布 -->
    <div class="section-header" style="margin-top: 40px">
      <span class="section-label">ORDER STATUS</span>
      <h3 class="section-title">订单状态分布</h3>
    </div>
    <div class="status-bars">
      <div v-for="item in statusList" :key="item.key" class="status-bar-item">
        <div class="bar-header">
          <span class="bar-label">{{ item.label }}</span>
          <span class="bar-count">{{ stats[item.key] || 0 }}</span>
        </div>
        <div class="bar-track">
          <div class="bar-fill" :style="{ width: getBarWidth(stats[item.key]), background: item.color }"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getDashboardStats } from '@/api/admin'
import {
  DocumentChecked, Money, Setting, Box, Monitor,
  Wallet, CreditCard, CircleCheck, ArrowRight, Calendar
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const stats = ref({})
const currentTime = ref('')

const statCards = [
  { key: 'pendingReviews', label: '待终审', icon: 'DocumentChecked', bg: 'rgba(102, 126, 234, 0.15)', color: '#667eea', action: () => router.push('/admin/reviews') },
  { key: 'pendingQuotes', label: '待报价', icon: 'Money', bg: 'rgba(245, 87, 108, 0.15)', color: '#f5576c', action: () => router.push('/admin/quotes') },
  { key: 'depositPending', label: '待付定金', icon: 'Wallet', bg: 'rgba(254, 225, 64, 0.15)', color: '#fee140' },
  { key: 'inProduction', label: '生产中', icon: 'Setting', bg: 'rgba(67, 233, 123, 0.15)', color: '#43e97b', action: () => router.push('/admin/production') },
  { key: 'pendingQc', label: '待质检', icon: 'Monitor', bg: 'rgba(79, 172, 254, 0.15)', color: '#4facfe', action: () => router.push('/admin/production') },
  { key: 'balancePending', label: '待付尾款', icon: 'CreditCard', bg: 'rgba(240, 147, 251, 0.15)', color: '#f093fb' },
  { key: 'pendingShipping', label: '待发货', icon: 'Box', bg: 'rgba(168, 237, 234, 0.15)', color: '#a8edea', action: () => router.push('/admin/production') },
  { key: 'shipped', label: '已发货', icon: 'Box', bg: 'rgba(56, 249, 215, 0.15)', color: '#38f9d7' },
  { key: 'completed', label: '已完成', icon: 'CircleCheck', bg: 'rgba(67, 233, 123, 0.15)', color: '#43e97b' }
]

const statusList = [
  { key: 'pendingReviews', label: '待终审', color: 'linear-gradient(90deg, #667eea, #764ba2)' },
  { key: 'pendingQuotes', label: '待报价', color: 'linear-gradient(90deg, #f5576c, #f093fb)' },
  { key: 'depositPending', label: '待付定金', color: 'linear-gradient(90deg, #fee140, #fa709a)' },
  { key: 'inProduction', label: '生产中', color: 'linear-gradient(90deg, #43e97b, #38f9d7)' },
  { key: 'pendingQc', label: '待质检', color: 'linear-gradient(90deg, #4facfe, #00f2fe)' },
  { key: 'balancePending', label: '待付尾款', color: 'linear-gradient(90deg, #f093fb, #f5576c)' },
  { key: 'pendingShipping', label: '待发货', color: 'linear-gradient(90deg, #a8edea, #fed6e3)' },
  { key: 'shipped', label: '已发货', color: 'linear-gradient(90deg, #38f9d7, #43e97b)' },
  { key: 'completed', label: '已完成', color: 'linear-gradient(90deg, #43e97b, #38f9d7)' }
]

const totalPending = computed(() => {
  return (stats.value.pendingReviews || 0) + (stats.value.pendingQuotes || 0) +
    (stats.value.depositPending || 0) + (stats.value.inProduction || 0) +
    (stats.value.pendingQc || 0) + (stats.value.pendingShipping || 0)
})

function getBarWidth(val) {
  const max = Math.max(...statusList.map(s => stats.value[s.key] || 0), 1)
  return `${((val || 0) / max) * 100}%`
}

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    stats.value = res.data || {}
  } catch (e) {
    // ignore
  }
  updateTime()
  setInterval(updateTime, 60000)
})
</script>

<style scoped>
.admin-dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

/* 欢迎条 */
.welcome-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 32px;
  border-radius: 16px;
  margin-bottom: 24px;
}

.welcome-bar h2 {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 4px;
}

.welcome-bar p {
  font-size: 14px;
  color: #888;
}

.welcome-bar strong {
  color: #667eea;
}

.welcome-time {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #888;
  font-size: 14px;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 40px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.stat-card:hover {
  background: rgba(255, 255, 255, 0.07);
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #888;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: #fff;
}

.stat-arrow {
  color: #333;
}

/* Section Header */
.section-header {
  margin-bottom: 20px;
}

.section-label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #667eea;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  margin-top: 4px;
}

/* 快捷入口 */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 40px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.quick-card:hover {
  background: rgba(255, 255, 255, 0.07);
  transform: translateY(-2px);
}

.quick-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.quick-info h4 {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 4px;
}

.quick-info p {
  font-size: 13px;
  color: #888;
}

/* 状态分布 */
.status-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-bar-item {
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 10px;
}

.bar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.bar-label {
  font-size: 14px;
  color: #ccc;
}

.bar-count {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
}

.bar-track {
  height: 6px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;
}

@media (max-width: 768px) {
  .stats-grid { grid-template-columns: 1fr; }
  .quick-grid { grid-template-columns: 1fr; }
}
</style>
