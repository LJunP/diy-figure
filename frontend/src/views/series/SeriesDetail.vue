<template>
  <div class="series-detail-page" v-loading="loading">
    <!-- 系列信息 -->
    <div class="detail-header glass-strong" v-reveal>
      <div class="header-glow"></div>
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
        <el-button round @click="openRenameDialog">
          <el-icon class="el-icon--left"><EditPen /></el-icon>
          重命名
        </el-button>
        <el-button round type="danger" plain @click="handleDeleteSeries">
          <el-icon class="el-icon--left"><Delete /></el-icon>
          删除系列
        </el-button>
        <el-button round @click="showCreateDialog = true">
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

    <!-- 重命名系列弹窗 -->
    <el-dialog v-model="showRenameDialog" title="修改系列信息" width="460px">
      <el-form label-position="top">
        <el-form-item label="系列名称">
          <el-input v-model="renameForm.name" size="large" maxlength="100" show-word-limit placeholder="例如:星空精灵系列" />
        </el-form-item>
        <el-form-item label="尺寸档位">
          <el-radio-group v-model="renameForm.sizeTier" size="large">
            <el-radio-button value="SMALL">小型 (约 6cm)</el-radio-button>
            <el-radio-button value="MEDIUM">中型 (约 10cm)</el-radio-button>
            <el-radio-button value="LARGE">大型 (约 15cm)</el-radio-button>
            <el-radio-button value="STANDARD">STANDARD</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <p class="dialog-note">规格档位({{ tierLabel(detail.specTier) }})创建后不可更改,如需调整请新建系列。</p>
      <template #footer>
        <el-button @click="showRenameDialog = false">取消</el-button>
        <el-button type="primary" :loading="renaming" @click="handleRename">保存</el-button>
      </template>
    </el-dialog>

    <!-- 进度条 -->
    <div class="progress-bar-section glass-card" v-reveal="{ delay: 0.1 }">
      <div class="progress-header">
        <span class="progress-label">设计进度</span>
        <span class="progress-count">{{ detail.finalizedCount || 0 }} / {{ requiredCount }} 已定稿</span>
      </div>
      <el-progress :percentage="progressPercent" :status="progressStatus" :stroke-width="14" />
    </div>

    <!-- 画布列表 -->
    <div class="canvas-section">
      <div class="section-header" v-reveal>
        <h2 class="section-title">角色设计画布</h2>
        <span class="section-sub">点击画布进入 AI 设计工作台</span>
      </div>

      <div v-if="!detail.canvases || detail.canvases.length === 0" class="empty-canvases glass-card" v-reveal>
        <div class="empty-orb">
          <el-icon :size="36"><Picture /></el-icon>
        </div>
        <p>还没有画布,点击右上角「新建画布」开始设计</p>
        <el-button round class="btn-liquid" @click="showCreateDialog = true">
          <el-icon class="el-icon--left"><Plus /></el-icon>
          新建画布
        </el-button>
      </div>

      <div v-else class="canvas-grid">
        <div
          v-for="(c, i) in detail.canvases"
          :key="c.id"
          class="canvas-card glass-card"
          v-reveal="{ index: i }"
          @click="openCanvas(c.id)"
        >
          <div class="canvas-thumb">
            <el-image v-if="c.firstConceptImage" :src="c.firstConceptImage" fit="cover" class="thumb-img" />
            <div v-else class="thumb-placeholder">
              <el-icon :size="32" color="#3a3a48"><Picture /></el-icon>
            </div>
            <div class="canvas-status-badge" :class="c.status === 'FINALIZED' ? 'finalized' : 'designing'">
              {{ c.status === 'FINALIZED' ? '已定稿' : '设计中' }}
            </div>
            <el-icon v-if="c.locked" class="lock-badge" color="#f87171" :size="20"><Lock /></el-icon>
          </div>
          <div class="canvas-card-body">
            <span class="canvas-title">{{ c.name || `画布 #${c.id}` }}</span>
            <span class="canvas-actions">
              <span class="canvas-arrow">进入设计 →</span>
              <el-tooltip content="删除画布" placement="top" :show-after="300">
                <el-button
                  class="canvas-del"
                  link
                  type="danger"
                  @click.stop="handleDeleteCanvas(c)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建画布弹窗 -->
    <el-dialog v-model="showCreateDialog" title="新建画布" width="440px">
      <p style="color: var(--text-3); margin-bottom: 16px">给这个角色设计起个名字,稍后你将与 AI 对话来设计这个角色</p>
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
import { getSeriesDetail, updateSeries, deleteSeries } from '@/api/series'
import { createCanvas, deleteCanvas } from '@/api/canvas'
import { createOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Picture, Lock, ShoppingCart, EditPen, Delete } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref({})
const showCreateDialog = ref(false)
const newCanvasName = ref('')
const creating = ref(false)
const showRenameDialog = ref(false)
const renaming = ref(false)
const renameForm = ref({ name: '', sizeTier: '' })

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

function openRenameDialog() {
  renameForm.value = { name: detail.value.name || '', sizeTier: detail.value.sizeTier || '' }
  showRenameDialog.value = true
}

async function handleRename() {
  const name = renameForm.value.name.trim()
  const sizeTier = renameForm.value.sizeTier.trim()
  if (!name) { ElMessage.warning('请输入系列名称'); return }
  if (!sizeTier) { ElMessage.warning('请输入尺寸档位'); return }
  renaming.value = true
  try {
    // 后端要求 name 与 sizeTier 同时提交,规格档位创建后不可更改
    await updateSeries(route.params.id, { name, sizeTier })
    ElMessage.success('已保存')
    showRenameDialog.value = false
    await loadDetail()
  } finally {
    renaming.value = false
  }
}

async function handleDeleteSeries() {
  try {
    await ElMessageBox.confirm(
      `确认删除系列「${detail.value.name}」吗?系列下的画布会一并删除,此操作不可撤销。`,
      '删除系列',
      { type: 'warning', confirmButtonText: '确认删除', confirmButtonClass: 'el-button--danger' }
    )
  } catch { return }  // 用户取消

  try {
    await deleteSeries(route.params.id)
    ElMessage.success('系列已删除')
    router.push('/series')
  } catch {
    // 已产生订单的系列会被后端拒绝(409),错误提示由响应拦截器统一弹出
  }
}

async function handleDeleteCanvas(canvas) {
  const label = canvas.name || `画布 #${canvas.id}`
  try {
    await ElMessageBox.confirm(
      `确认删除「${label}」吗?删除后无法恢复。`,
      '删除画布',
      { type: 'warning', confirmButtonText: '确认删除', confirmButtonClass: 'el-button--danger' }
    )
  } catch { return }

  try {
    await deleteCanvas(canvas.id)
    ElMessage.success('画布已删除')
    await loadDetail()
  } catch {
    // 已锁定 / 已被订单引用的画布会被后端拒绝,提示由响应拦截器统一处理
  }
}

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
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}

.header-glow {
  position: absolute;
  top: -40%; right: -10%;
  width: 50%; height: 80%;
  background: radial-gradient(circle, rgba(109, 124, 255, 0.14), transparent 60%);
  filter: blur(40px);
  pointer-events: none;
}

.series-name {
  font-size: 26px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
  position: relative;
}

.series-tags {
  display: flex;
  gap: 8px;
  position: relative;
}

.tag-chip {
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
}

.tag-chip.spec { background: rgba(109, 124, 255, 0.15); color: var(--brand-1); }
.tag-chip.size { background: rgba(255, 255, 255, 0.08); color: var(--text-2); }
.tag-chip.ready { background: rgba(52, 211, 153, 0.15); color: var(--brand-green); }
.tag-chip.designing { background: rgba(251, 191, 36, 0.15); color: var(--brand-gold); }

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
  position: relative;
}

/* 进度条 */
.progress-bar-section {
  padding: 20px 32px;
  border-radius: var(--radius-lg);
  margin-bottom: 32px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.progress-label { font-size: 14px; color: var(--text-2); }
.progress-count { font-size: 14px; color: var(--brand-1); font-weight: 600; }

/* 画布区 */
.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.section-title { font-size: 20px; font-weight: 700; color: #fff; }
.section-sub { font-size: 13px; color: var(--text-4); }

.empty-canvases {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 24px;
  border-radius: var(--radius-lg);
  gap: 16px;
}

.empty-orb {
  width: 80px; height: 80px;
  border-radius: 50%;
  background: rgba(109, 124, 255, 0.1);
  display: flex; align-items: center; justify-content: center;
  color: var(--brand-1);
  box-shadow: 0 0 32px rgba(109, 124, 255, 0.12);
  animation: float 4s ease-in-out infinite;
}

.empty-canvases p { color: var(--text-3); font-size: 14px; }

.canvas-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.canvas-card {
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
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
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  backdrop-filter: blur(8px);
}

.canvas-status-badge.finalized { background: rgba(52, 211, 153, 0.85); color: #fff; }
.canvas-status-badge.designing { background: rgba(251, 191, 36, 0.85); color: #1a1a1a; }

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

.canvas-title { font-size: 14px; color: var(--text-2); }
.canvas-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}
.canvas-arrow {
  font-size: 13px;
  color: var(--brand-1);
  transition: transform 0.25s var(--ease-out);
}
.canvas-card:hover .canvas-arrow { transform: translateX(4px); }

/* 删除按钮常驻但压暗,悬停卡片时恢复 —— 不用 opacity:0 完全隐藏:
   那样键盘用户和屏幕阅读器根本发现不了这个操作 */
.canvas-del {
  opacity: 0.45;
  transition: opacity 0.2s var(--ease-out);
  padding: 2px;
  height: auto;
}
.canvas-card:hover .canvas-del,
.canvas-del:focus-visible { opacity: 1; }

.dialog-note {
  margin: 0;
  font-size: 12px;
  color: var(--text-3);
  line-height: 1.6;
}

@media (max-width: 768px) {
  .canvas-grid { grid-template-columns: repeat(2, 1fr); }
  .detail-header { flex-direction: column; gap: 16px; }
  .header-actions { flex-wrap: wrap; }
}
</style>
