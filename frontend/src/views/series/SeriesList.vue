<template>
  <div class="series-list-page">
    <!-- 页头 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">我的系列</h1>
        <p class="page-desc">管理你的原创手办设计系列</p>
      </div>
      <el-button type="primary" round size="large" @click="$router.push('/series/create')">
        <el-icon class="el-icon--left"><Plus /></el-icon>
        创建系列
      </el-button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-grid">
      <div v-for="i in 3" :key="i" class="skeleton-card"></div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="series.length === 0" class="empty-state glass">
      <el-icon :size="64" color="#333"><FolderOpened /></el-icon>
      <h3>还没有系列</h3>
      <p>创建你的第一个原创手办设计系列,开始 AI 对话设计之旅</p>
      <el-button type="primary" round size="large" @click="$router.push('/series/create')">
        <el-icon class="el-icon--left"><Plus /></el-icon>
        创建第一个系列
      </el-button>
    </div>

    <!-- 系列卡片 -->
    <div v-else class="series-grid">
      <div v-for="s in series" :key="s.id" class="series-card" @click="$router.push(`/series/${s.id}`)">
        <div class="card-visual" :style="{ background: getGradient(s.id) }">
          <span class="card-spec">{{ tierLabel(s.specTier) }}</span>
          <span class="card-size">{{ s.sizeTier }}</span>
        </div>
        <div class="card-body">
          <div class="card-header-row">
            <h3 class="card-name">{{ s.name }}</h3>
            <el-tag size="small" :type="s.designStatus === 'READY_FOR_QUOTE' ? 'success' : 'info'" effect="dark">
              {{ s.designStatus === 'READY_FOR_QUOTE' ? '可提交报价' : '设计中' }}
            </el-tag>
          </div>
          <div class="card-footer">
            <span class="card-time">
              <el-icon><Clock /></el-icon>
              {{ formatDate(s.createdAt) }}
            </span>
            <span class="card-arrow">查看详情 →</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listSeries } from '@/api/series'
import { Plus, FolderOpened, Clock } from '@element-plus/icons-vue'

const loading = ref(false)
const series = ref([])

const tiers = {
  LIGHT: { label: '轻量(6选4)', type: 'primary' },
  CLASSIC: { label: '经典(9选6)', type: 'success' },
  COLLECTION: { label: '收藏(12选8)', type: 'warning' }
}

function tierLabel(t) { return tiers[t]?.label || t }

const gradients = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)'
]

function getGradient(id) {
  return gradients[id % gradients.length]
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleString('zh-CN', { dateStyle: 'short', timeStyle: 'short' })
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await listSeries()
    series.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.series-list-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
}

.page-desc {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

/* 骨架屏 */
.loading-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.skeleton-card {
  height: 280px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  animation: pulse 1.5s ease-in-out infinite;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  border-radius: 16px;
  text-align: center;
}

.empty-state h3 {
  font-size: 20px;
  color: #fff;
  margin: 20px 0 8px;
}

.empty-state p {
  font-size: 14px;
  color: #666;
  margin-bottom: 24px;
}

/* 系列卡片 */
.series-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.series-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
}

.series-card:hover {
  transform: translateY(-4px);
  border-color: rgba(102, 126, 234, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.card-visual {
  height: 140px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 12px;
  position: relative;
  overflow: hidden;
}

.card-visual::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 30% 20%, rgba(255,255,255,0.15), transparent 50%);
}

.card-spec, .card-size {
  position: relative;
  z-index: 1;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  text-shadow: 0 1px 4px rgba(0,0,0,0.3);
}

.card-body {
  padding: 20px;
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-name {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
}

.card-arrow {
  font-size: 13px;
  color: #667eea;
}

@media (max-width: 768px) {
  .series-grid, .loading-grid { grid-template-columns: 1fr; }
}
</style>
