<template>
  <div class="review-list-page">
    <!-- 页头 -->
    <div class="page-header" v-reveal>
      <div>
        <h2 class="page-title">待终审订单</h2>
        <p class="page-desc">审核用户提交的原创角色设计,通过后进入报价阶段</p>
      </div>
      <el-button round @click="loadData">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 订单列表 -->
    <div v-loading="loading" class="order-list">
      <div v-if="orders.length === 0 && !loading" class="empty glass-card" v-reveal>
        <div class="empty-orb"><el-icon :size="48"><DocumentChecked /></el-icon></div>
        <p>没有待终审的订单</p>
      </div>

      <div v-for="(order, i) in orders" :key="order.id" class="order-card glass-card" v-reveal="{ index: i }">
        <div class="oc-header">
          <span class="oc-id">#{{ order.id }}</span>
          <el-tag type="warning" effect="dark" size="small">终审中</el-tag>
        </div>
        <div class="oc-body">
          <div class="oc-info">
            <span class="oi-label">用户 ID</span>
            <span class="oi-value">{{ order.userId }}</span>
          </div>
          <div class="oc-info">
            <span class="oi-label">系列 ID</span>
            <span class="oi-value">{{ order.seriesId }}</span>
          </div>
          <div class="oc-info">
            <span class="oi-label">创建时间</span>
            <span class="oi-value">{{ order.createdAt }}</span>
          </div>
        </div>
        <div class="oc-actions">
          <el-button type="primary" round size="small" @click="openReviewDialog(order)">查看 & 审核</el-button>
        </div>
      </div>
    </div>

    <!-- 审核弹窗 -->
    <el-dialog v-model="reviewDialogVisible" title="终审操作" width="820px" top="6vh">
      <div class="review-content" v-loading="detailLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单 ID">{{ currentOrder?.id }}</el-descriptions-item>
          <el-descriptions-item label="用户 ID">{{ currentOrder?.userId }}</el-descriptions-item>
          <el-descriptions-item label="系列">{{ orderDetail?.series?.name || '—' }}</el-descriptions-item>
          <el-descriptions-item label="规格档位">{{ tierLabel(orderDetail?.series?.specTier) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(currentOrder?.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="设计数量">
            {{ orderDetail?.canvases?.length || 0 }} 个
          </el-descriptions-item>
        </el-descriptions>

        <!-- ★ 终审的核心依据:必须看到全部定稿设计才能判断侵权风险 -->
        <div class="design-section">
          <div class="design-head">
            <h4 class="design-title">待审设计稿</h4>
            <span class="design-hint">请逐张确认是否存在可辨识真实人物或已有版权 IP 的元素</span>
          </div>

          <div v-if="!detailLoading && !(orderDetail?.canvases || []).length" class="design-empty">
            该订单没有关联画布
          </div>

          <div v-else class="design-grid">
            <div v-for="c in orderDetail?.canvases || []" :key="c.canvasId" class="design-card">
              <el-image
                v-if="c.firstConceptImage"
                :src="c.firstConceptImage"
                fit="cover"
                class="design-img"
                :preview-src-list="[c.firstConceptImage]"
                preview-teleported
              />
              <div v-else class="design-img no-img"><el-icon :size="22"><Picture /></el-icon></div>
              <div class="design-body">
                <span class="design-name">{{ c.name || `画布 #${c.canvasId}` }}</span>
                <el-tag :type="c.status === 'FINALIZED' ? 'success' : 'info'" size="small" effect="dark">
                  {{ c.status === 'FINALIZED' ? '已定稿' : '设计中' }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>

        <el-divider />

        <el-form label-position="top">
          <el-form-item label="审核结果">
            <el-radio-group v-model="reviewForm.result">
              <el-radio-button value="APPROVED">通过</el-radio-button>
              <el-radio-button value="REJECTED">拒绝</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="reviewForm.result === 'REJECTED'" label="拒绝原因">
            <el-input v-model="reviewForm.rejectReason" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleReview">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getPendingReviews, reviewOrder, getAdminOrderDetail } from '@/api/admin'
import { ElMessage } from 'element-plus'
import { Refresh, DocumentChecked, Picture } from '@element-plus/icons-vue'

const tiers = {
  LIGHT: '轻量(6选4)',
  CLASSIC: '经典(9选6)',
  COLLECTION: '收藏(12选8)'
}
function tierLabel(t) { return tiers[t] || t || '—' }

function formatTime(d) {
  if (!d) return '—'
  return new Date(d).toLocaleString('zh-CN', { dateStyle: 'short', timeStyle: 'short' })
}

const loading = ref(false)
const submitting = ref(false)
const detailLoading = ref(false)
const orders = ref([])
const reviewDialogVisible = ref(false)
const currentOrder = ref(null)
const orderDetail = ref(null)
const reviewForm = reactive({ result: 'APPROVED', rejectReason: '' })

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPendingReviews()
    orders.value = res.data || []
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const openReviewDialog = async (order) => {
  currentOrder.value = order
  orderDetail.value = null
  reviewForm.result = 'APPROVED'
  reviewForm.rejectReason = ''
  reviewDialogVisible.value = true

  // 终审必须能看到设计稿,这里单独拉一次订单详情
  detailLoading.value = true
  try {
    const res = await getAdminOrderDetail(order.id)
    orderDetail.value = res.data
  } catch (e) {
    // 详情拉取失败不阻塞审核动作,但要让审核人知道看到的信息不完整
    ElMessage.warning('设计稿加载失败,请刷新后重试')
  } finally {
    detailLoading.value = false
  }
}

const handleReview = async () => {
  if (reviewForm.result === 'REJECTED' && !String(reviewForm.rejectReason || '').trim()) {
    ElMessage.warning('拒绝时必须填写理由')
    return
  }
  submitting.value = true
  try {
    await reviewOrder(currentOrder.value.id, {
      result: reviewForm.result,
      rejectReason: reviewForm.rejectReason
    })
    ElMessage.success(reviewForm.result === 'APPROVED' ? '审核通过' : '已拒绝')
    reviewDialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.review-list-page { max-width: 1000px; margin: 0 auto; }

.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.page-title {
  font-size: 22px; font-weight: 700;
  background: var(--gradient-brand); background-size: 200% 200%;
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
  animation: gradientShift 6s ease infinite;
}
.page-desc { font-size: 14px; color: var(--text-3); margin-top: 4px; }

.empty { display: flex; flex-direction: column; align-items: center; padding: 60px; border-radius: var(--radius-lg); gap: 16px; }
.empty-orb {
  width: 80px; height: 80px; border-radius: 50%;
  background: rgba(109, 124, 255, 0.1);
  display: flex; align-items: center; justify-content: center;
  color: var(--brand-1);
  box-shadow: 0 0 32px rgba(109, 124, 255, 0.12);
  animation: float 4s ease-in-out infinite;
}
.empty p { color: var(--text-3); }

.order-list { display: flex; flex-direction: column; gap: 16px; }

.order-card { padding: 24px; border-radius: 14px; }
.oc-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.oc-id { font-size: 18px; font-weight: 700; color: #fff; }
.oc-body { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 16px; }
.oc-info { display: flex; flex-direction: column; gap: 4px; }
.oi-label { font-size: 12px; color: var(--text-4); }
.oi-value { font-size: 14px; color: var(--text-2); }
.oc-actions { display: flex; gap: 8px; }

.review-content { color: var(--text-2); }

/* 设计稿区:终审判断侵权风险的唯一依据 */
.design-section { margin-top: 20px; }
.design-head {
  display: flex; align-items: baseline; justify-content: space-between;
  gap: 12px; margin-bottom: 12px; flex-wrap: wrap;
}
.design-title { margin: 0; font-size: 15px; color: var(--text-1); font-weight: 600; }
.design-hint { font-size: 12px; color: var(--text-3); }
.design-empty { font-size: 13px; color: var(--text-3); padding: 20px 0; text-align: center; }
.design-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}
.design-card {
  border-radius: 10px; overflow: hidden;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
}
.design-img { width: 100%; height: 120px; display: block; }
.design-img.no-img {
  display: flex; align-items: center; justify-content: center;
  color: var(--text-4); background: rgba(255, 255, 255, 0.02);
}
.design-body {
  display: flex; align-items: center; justify-content: space-between;
  gap: 6px; padding: 8px 10px;
}
.design-name {
  font-size: 13px; color: var(--text-2);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
</style>
