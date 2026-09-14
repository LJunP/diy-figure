<template>
  <div class="admin-dashboard">
    <!-- 欢迎条 -->
    <div class="welcome-bar glass-strong" v-reveal>
      <div class="welcome-glow"></div>
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
      <div
        v-for="(card, i) in statCards"
        :key="card.key"
        class="stat-card glass-card"
        v-reveal="{ index: i }"
        @click="card.action?.()"
      >
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
    <div class="section-header" v-reveal>
      <span class="section-label">QUICK ACCESS</span>
      <h3 class="section-title">快捷入口</h3>
    </div>
    <div class="quick-grid">
      <div
        v-for="(q, i) in quickCards"
        :key="i"
        class="quick-card glass-card"
        v-reveal="{ index: i }"
        @click="$router.push(q.to)"
      >
        <div class="quick-icon" :style="{ background: q.bg, color: q.color }">
          <el-icon :size="28"><component :is="q.icon" /></el-icon>
        </div>
        <div class="quick-info">
          <h4>{{ q.title }}</h4>
          <p>{{ q.desc }}</p>
        </div>
        <el-icon class="stat-arrow"><ArrowRight /></el-icon>
      </div>
    </div>

    <!-- 订单状态分布 -->
    <div class="section-header" style="margin-top: 40px" v-reveal>
      <span class="section-label">ORDER STATUS</span>
      <h3 class="section-title">订单状态分布</h3>
    </div>
    <div class="status-bars">
      <div v-for="(item, i) in statusList" :key="item.key" class="status-bar-item glass-card" v-reveal="{ index: i }">
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
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
  { key: 'pendingReviews', label: '待终审', icon: 'DocumentChecked', bg: 'rgba(109, 124, 255, 0.15)', color: '#6d7cff', action: () => router.push('/admin/reviews') },
  { key: 'pendingQuotes', label: '待报价', icon: 'Money', bg: 'rgba(236, 72, 153, 0.15)', color: '#ec4899', action: () => router.push('/admin/quotes') },
  { key: 'depositPending', label: '待付定金', icon: 'Wallet', bg: 'rgba(251, 191, 36, 0.15)', color: '#fbbf24' },
  { key: 'inProduction', label: '生产中', icon: 'Setting', bg: 'rgba(52, 211, 153, 0.15)', color: '#34d399', action: () => router.push('/admin/production') },
  { key: 'pendingQc', label: '待质检', icon: 'Monitor', bg: 'rgba(34, 211, 238, 0.15)', color: '#22d3ee', action: () => router.push('/admin/production') },
  { key: 'balancePending', label: '待付尾款', icon: 'CreditCard', bg: 'rgba(168, 85, 247, 0.15)', color: '#a855f7' },
  { key: 'pendingShipping', label: '待发货', icon: 'Box', bg: 'rgba(168, 237, 234, 0.15)', color: '#a8edea', action: () => router.push('/admin/production') },
  { key: 'shipped', label: '已发货', icon: 'Box', bg: 'rgba(56, 249, 215, 0.15)', color: '#38f9d7' },
  { key: 'completed', label: '已完成', icon: 'CircleCheck', bg: 'rgba(52, 211, 153, 0.15)', color: '#34d399' }
]

const quickCards = [
  { title: '待终审管理', desc: '审核用户提交的角色设计', icon: 'DocumentChecked', to: '/admin/reviews', bg: 'rgba(109, 124, 255, 0.15)', color: '#6d7cff' },
  { title: '待报价管理', desc: '为终审通过的订单定价', icon: 'Money', to: '/admin/quotes', bg: 'rgba(236, 72, 153, 0.15)', color: '#ec4899' },
  { title: '生产物流管理', desc: '生产、质检、发货全流程', icon: 'Setting', to: '/admin/production', bg: 'rgba(52, 211, 153, 0.15)', color: '#34d399' }
]

const statusList = [
  { key: 'pendingReviews', label: '待终审', color: 'linear-gradient(90deg, #6d7cff, #a855f7)' },
  { key: 'pendingQuotes', label: '待报价', color: 'linear-gradient(90deg, #ec4899, #f093fb)' },
  { key: 'depositPending', label: '待付定金', color: 'linear-gradient(90deg, #fbbf24, #fa709a)' },
  { key: 'inProduction', label: '生产中', color: 'linear-gradient(90deg, #34d399, #38f9d7)' },
  { key: 'pendingQc', label: '待质检', color: 'linear-gradient(90deg, #22d3ee, #34d399)' },
  { key: 'balancePending', label: '待付尾款', color: 'linear-gradient(90deg, #a855f7, #ec4899)' },
  { key: 'pendingShipping', label: '待发货', color: 'linear-gradient(90deg, #a8edea, #fed6e3)' },
  { key: 'shipped', label: '已发货', color: 'linear-gradient(90deg, #38f9d7, #34d399)' },
  { key: 'completed', label: '已完成', color: 'linear-gradient(90deg, #34d399, #22d3ee)' }
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
  timer = setInterval(updateTime, 60000)
})

let timer
onBeforeUnmount(() => { if (timer) clearInterval(timer) })
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
  border-radius: var(--radius-lg);
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
}

.welcome-glow {
  position: absolute;
  top: -50%; right: -10%;
  width: 50%; height: 80%;
  background: radial-gradient(circle, rgba(109, 124, 255, 0.14), transparent 60%);
  filter: blur(40px);
  pointer-events: none;
}

.welcome-bar h2 { font-size: 22px; font-weight: 700; color: #fff; margin-bottom: 4px; position: relative; }
.welcome-bar p { font-size: 14px; color: var(--text-3); position: relative; }
.welcome-bar strong {
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-time {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-3);
  font-size: 14px;
  position: relative;
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

.stat-info { flex: 1; }
.stat-label { font-size: 13px; color: var(--text-3); margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 800; color: #fff; }

.stat-arrow { color: var(--text-4); transition: transform 0.25s var(--ease-out); }
.stat-card:hover .stat-arrow { transform: translateX(4px); color: var(--brand-1); }

/* Section Header */
.section-header { margin-bottom: 20px; }
.section-label { font-size: 12px; font-weight: 600; letter-spacing: 2px; color: var(--brand-1); }
.section-title { font-size: 20px; font-weight: 700; color: #fff; margin-top: 4px; }

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

.quick-info h4 { font-size: 16px; font-weight: 600; color: #fff; margin-bottom: 4px; }
.quick-info p { font-size: 13px; color: var(--text-3); }

/* 状态分布 */
.status-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-bar-item {
  padding: 12px 20px;
  border-radius: 12px;
}

.bar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.bar-label { font-size: 14px; color: var(--text-2); }
.bar-count { font-size: 16px; font-weight: 700; color: #fff; }

.bar-track {
  height: 8px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s var(--ease-out);
  box-shadow: 0 0 12px rgba(109, 124, 255, 0.2);
}

@media (max-width: 768px) {
  .stats-grid { grid-template-columns: 1fr; }
  .quick-grid { grid-template-columns: 1fr; }
}
</style>
