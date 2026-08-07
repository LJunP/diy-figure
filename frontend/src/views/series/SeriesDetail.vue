<template>
  <div class="series-detail-page" v-loading="loading">
    <!-- 系列信息 -->
    <div class="detail-header glass">
      <div class="header-left">
        <h1 class="series-name">{{ detail.name }}</h1>
        <div class="series-tags">
          <span class="tag-chip spec">{{ tierLabel(detail.specTier) }}</span>
          <span class="tag-chip size">{{ detail.sizeTier }}</span>
          <span class="tag-chip" :class="detail.designStatus === 'READY_FOR_QUOTE' ? 'ready' : 'designing'">
            {{ detail.designStatus === 'READY_FOR_QUOTE' ? '✓ 可提交报价' : '设计中' }}
          </span>
        </div>
      </div>
      <div class="header-actions">
        <el-button type="primary" round @click="showCreateDialog = true">
          <el-icon class="el-icon--left"><Plus /></el-icon>
          新建画布
        </el-button>
        <el-button
          v-if="detail.designStatus === 'READY_FOR_QUOTE'"
          type="success"
          round
          size="large"
          @click="handleCreateOrder"
        >
          <el-icon class="el-icon--left"><ShoppingCart /></el-icon>
          创建订单
        </el-button>
      </div>
    </div>

    <!-- 进度条 -->
    <div class="progress-bar-section glass">
      <div class="progress-header">
        <span class="progress-label">设计进度</span>
        <span class="progress-count">{{ detail.finalizedCount || 0 }} / {{ requiredCount }} 已定稿</span>
      </div>
      <el-progress :percentage="progressPercent" :status="progressStatus" :stroke-width="12" />
    </div>

    <!-- 画布列表 -->
    <div class="canvas-section">
      <div class="section-header">
        <h2 class="section-title">角色设计画布</h2>
        <span class="section-sub">点击画布进入 AI 设计工作台</span>
      </div>

      <div v-if="!detail.canvases || detail.canvases.length === 0" class="empty-canvases glass">
        <el-icon :size="48" color="#333"><Picture /></el-icon>
        <p>还没有画布,点击右上角「新建画布」开始设计</p>
        <el-button type="primary" round @click="showCreateDialog = true">
          <el-icon class="el-icon--left"><Plus /></el-icon>
          新建画布
        </el-button>
      </div>

      <div v-else class="canvas-grid">
        <div v-for="c in detail.canvases" :key="c.id" class="canvas-card" @click="openCanvas(c.id)">
          <div class="canvas-thumb">
            <el-image v-if="c.firstConceptImage" :src="c.firstConceptImage" fit="cover" class="thumb-img" />
            <div v-else class="thumb-placeholder">
              <el-icon :size="32" color="#444"><Picture /></el-icon>
            </div>
            <div class="canvas-status-badge" :class="c.status === 'FINALIZED' ? 'finalized' : 'designing'">
              {{ c.status === 'FINALIZED' ? '已定稿' : '设计中' }}
            </div>
            <el-icon v-if="c.locked" class="lock-badge" color="#f56c6c" :size="20"><Lock /></el-icon>
          </div>
          <div class="canvas-card-body">
            <span class="canvas-title">画布 #{{ c.id }}</span>
            <span class="canvas-arrow">进入设计 →</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建画布弹窗 -->
    <el-dialog v-model="showCreateDialog" title="新建画布" width="440px">
      <p style="color: #888; margin-bottom: 16px">给这个角色设计起个名字,稍后你将与 AI 对话来设计这个角色</p>
      <el-input v-model="newCanvasName" placeholder="例如:星光精灵" size="large" />
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateCanvas">创建并开始设计</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSeriesDetail } from '@/api/series'
import { createCanvas } from '@/api/canvas'
import { createOrder } from '@/api/order'
import { ElMessage } from 'element-plus'
import { Plus, Picture, Lock, ShoppingCart } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref({})
const showCreateDialog = ref(false)
const newCanvasName = ref('')
const creating = ref(false)

const tiers = { LIGHT: '轻量(6选4)', CLASSIC: '经典(9选6)', COLLECTION: '收藏(12选8)' }
function tierLabel(t) { return tiers[t] || t }

const requiredCount = computed(() => {
  const t = detail.value.specTier
  if (t === 'LIGHT') return 6
  if (t === 'CLASSIC') return 9
  if (t === 'COLLECTION') return 12
  return 0
})

const progressPercent = computed(() => {
  if (!requiredCount.value) return 0
  return Math.min(100, Math.round((detail.value.finalizedCount || 0) / requiredCount.value * 100))
})

const progressStatus = computed(() => progressPercent.value >= 100 ? 'success' : '')

async function loadDetail() {
  loading.value = true
  try {
    const res = await getSeriesDetail(route.params.id)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

function openCanvas(id) { router.push(`/canvases/${id}`) }

async function handleCreateCanvas() {
  if (!newCanvasName.value.trim()) {
    ElMessage.warning('请输入画布名称')
    return
  }
  creating.value = true
  try {
    const res = await createCanvas(route.params.id, { name: newCanvasName.value })
    ElMessage.success('画布创建成功')
    showCreateDialog.value = false
    newCanvasName.value = ''
    router.push(`/canvases/${res.data.id}`)
  } finally {
    creating.value = false
  }
}

async function handleCreateOrder() {
  const finalizedCanvases = detail.value.canvases?.filter(c => c.status === 'FINALIZED') || []
  if (finalizedCanvases.length < requiredCount.value) {
    ElMessage.warning(`需要 ${requiredCount.value} 个已定稿画布,当前仅 ${finalizedCanvases.length} 个`)
    return
  }
  creating.value = true
  try {
    const canvasIds = finalizedCanvases.map(c => c.id)
    const res = await createOrder({ seriesId: route.params.id, canvasIds })
    ElMessage.success('订单创建成功')
    router.push(`/orders/${res.data.id}`)
  } catch (error) {
    ElMessage.error('订单创建失败: ' + (error.response?.data?.message || error.message))
  } finally {
    creating.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.series-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px;
}

/* 头部 */
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28px 32px;
  border-radius: 16px;
  margin-bottom: 20px;
}

.series-name {
  font-size: 26px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
}

.series-tags {
  display: flex;
  gap: 8px;
}

.tag-chip {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.tag-chip.spec { background: rgba(102, 126, 234, 0.15); color: #667eea; }
.tag-chip.size { background: rgba(255, 255, 255, 0.08); color: #ccc; }
.tag-chip.ready { background: rgba(67, 233, 123, 0.15); color: #43e97b; }
.tag-chip.designing { background: rgba(254, 225, 64, 0.15); color: #fee140; }

.header-actions {
  display: flex;
  gap: 12px;
}

/* 进度条 */
.progress-bar-section {
  padding: 20px 32px;
  border-radius: 16px;
  margin-bottom: 32px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.progress-label { font-size: 14px; color: #ccc; }
.progress-count { font-size: 14px; color: #667eea; font-weight: 600; }

/* 画布区 */
.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.section-title { font-size: 20px; font-weight: 700; color: #fff; }
.section-sub { font-size: 13px; color: #666; }

.empty-canvases {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 24px;
  border-radius: 16px;
  gap: 16px;
}

.empty-canvases p { color: #666; font-size: 14px; }

.canvas-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.canvas-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.canvas-card:hover {
  transform: translateY(-4px);
  border-color: rgba(102, 126, 234, 0.3);
}

.canvas-thumb {
  height: 160px;
  position: relative;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.02);
}

.thumb-img { width: 100%; height: 100%; }

.thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.canvas-status-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.canvas-status-badge.finalized { background: rgba(67, 233, 123, 0.8); color: #fff; }
.canvas-status-badge.designing { background: rgba(254, 225, 64, 0.8); color: #333; }

.lock-badge {
  position: absolute;
  top: 8px;
  right: 8px;
}

.canvas-card-body {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.canvas-title { font-size: 14px; color: #ccc; }
.canvas-arrow { font-size: 13px; color: #667eea; }

@media (max-width: 768px) {
  .canvas-grid { grid-template-columns: repeat(2, 1fr); }
  .detail-header { flex-direction: column; gap: 16px; }
}
</style>
