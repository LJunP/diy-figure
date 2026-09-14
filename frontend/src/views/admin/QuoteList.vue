<template>
  <div class="quote-list-page">
    <!-- 页头 -->
    <div class="page-header" v-reveal>
      <div>
        <h2 class="page-title">待报价订单</h2>
        <p class="page-desc">为终审通过的订单填写报价金额和预计交货日期</p>
      </div>
      <el-button round @click="loadData">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 订单列表 -->
    <div v-loading="loading" class="order-list">
      <div v-if="orders.length === 0 && !loading" class="empty glass-card" v-reveal>
        <div class="empty-orb"><el-icon :size="48"><Money /></el-icon></div>
        <p>没有待报价的订单</p>
      </div>

      <div v-for="(order, i) in orders" :key="order.id" class="order-card glass-card" v-reveal="{ index: i }">
        <div class="oc-header">
          <span class="oc-id">#{{ order.id }}</span>
          <el-tag type="danger" effect="dark" size="small">待报价</el-tag>
          <span v-if="order.quotedPrice" class="oc-price">¥{{ order.quotedPrice }}</span>
        </div>
        <div class="oc-body">
          <div class="oc-info"><span class="oi-label">用户 ID</span><span class="oi-value">{{ order.userId }}</span></div>
          <div class="oc-info"><span class="oi-label">系列 ID</span><span class="oi-value">{{ order.seriesId }}</span></div>
          <div class="oc-info"><span class="oi-label">创建时间</span><span class="oi-value">{{ order.createdAt }}</span></div>
        </div>
        <div class="oc-actions">
          <el-button type="primary" round size="small" @click="openQuoteDialog(order)">报价</el-button>
        </div>
      </div>
    </div>

    <!-- 报价弹窗 -->
    <el-dialog v-model="quoteDialogVisible" title="报价" width="820px" top="6vh">
      <div v-loading="detailLoading" class="quote-content">
        <!-- 报价必须知道档位与设计数量,否则没有定价依据 -->
        <el-descriptions :column="3" border>
          <el-descriptions-item label="订单 ID">{{ currentOrderId }}</el-descriptions-item>
          <el-descriptions-item label="系列">{{ orderDetail?.series?.name || '—' }}</el-descriptions-item>
          <el-descriptions-item label="规格档位">{{ tierLabel(orderDetail?.series?.specTier) }}</el-descriptions-item>
          <el-descriptions-item label="尺寸档位">{{ orderDetail?.series?.sizeTier || '—' }}</el-descriptions-item>
          <el-descriptions-item label="设计数量">
            {{ orderDetail?.canvases?.length || 0 }} 个
          </el-descriptions-item>
          <el-descriptions-item label="档位中签数">
            {{ expectedSelected }} 个
          </el-descriptions-item>
        </el-descriptions>

        <div class="design-section">
          <div class="design-head">
            <h4 class="design-title">设计稿</h4>
            <span class="design-hint">实际生产的是「中签」角色,定价时请以中签数量与复杂度为准</span>
          </div>

          <div v-if="!detailLoading && !(orderDetail?.canvases || []).length" class="design-empty">
            该订单没有关联画布
          </div>

          <div v-else class="design-grid">
            <div
              v-for="c in orderDetail?.canvases || []"
              :key="c.canvasId"
              class="design-card"
              :class="{ selected: c.lotteryResult === 'SELECTED' }"
            >
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
                <el-tag v-if="c.lotteryResult && c.lotteryResult !== 'UNDECIDED'" :type="c.lotteryResult === 'SELECTED' ? 'success' : 'info'" size="small" effect="dark">
                  {{ c.lotteryResult === 'SELECTED' ? '中签' : '未中签' }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>

        <el-divider />

        <el-form :model="quoteForm" label-position="top">
          <el-form-item label="报价金额 (元)">
            <el-input-number v-model="quoteForm.quotedPrice" :min="0" :precision="2" size="large" style="width: 100%" />
          </el-form-item>
          <el-form-item label="预计交货日期">
            <el-date-picker v-model="quoteForm.expectedDeliveryDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" size="large" style="width: 100%" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="quoteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleQuote">确认报价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getPendingQuotes, quoteOrder, getAdminOrderDetail } from '@/api/admin'
import { ElMessage } from 'element-plus'
import { Refresh, Money, Picture } from '@element-plus/icons-vue'

const tiers = {
  LIGHT: '轻量(6选4)',
  CLASSIC: '经典(9选6)',
  COLLECTION: '收藏(12选8)'
}
function tierLabel(t) { return tiers[t] || t || '—' }

const loading = ref(false)
const submitting = ref(false)
const detailLoading = ref(false)
const orders = ref([])
const quoteDialogVisible = ref(false)
const currentOrderId = ref(null)
const orderDetail = ref(null)
const quoteForm = reactive({ quotedPrice: 0, expectedDeliveryDate: '' })

const expectedSelected = computed(() => {
  const map = { LIGHT: 4, CLASSIC: 6, COLLECTION: 8 }
  return map[orderDetail.value?.series?.specTier] || 0
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPendingQuotes()
    orders.value = res.data || []
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const openQuoteDialog = async (order) => {
  currentOrderId.value = order.id
  orderDetail.value = null
  quoteForm.quotedPrice = order.quotedPrice || 0
  quoteForm.expectedDeliveryDate = order.expectedDeliveryDate || ''
  quoteDialogVisible.value = true

  // 定价需要知道档位、设计数量和中签数量,单独拉一次订单详情
  detailLoading.value = true
  try {
    const res = await getAdminOrderDetail(order.id)
    orderDetail.value = res.data
  } catch (e) {
    ElMessage.warning('设计稿加载失败,请刷新后重试')
  } finally {
    detailLoading.value = false
  }
}

const handleQuote = async () => {
  if (!quoteForm.quotedPrice || quoteForm.quotedPrice <= 0) {
    ElMessage.warning('请输入有效的报价金额')
    return
  }
  submitting.value = true
  try {
    await quoteOrder(currentOrderId.value, {
      quotedPrice: quoteForm.quotedPrice,
      expectedDeliveryDate: quoteForm.expectedDeliveryDate
    })
    ElMessage.success('报价成功')
    quoteDialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('报价失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.quote-list-page { max-width: 1000px; margin: 0 auto; }
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
  background: rgba(236, 72, 153, 0.1);
  display: flex; align-items: center; justify-content: center;
  color: var(--brand-3);
  box-shadow: 0 0 32px rgba(236, 72, 153, 0.12);
  animation: float 4s ease-in-out infinite;
}
.empty p { color: var(--text-3); }

.order-list { display: flex; flex-direction: column; gap: 16px; }
.order-card { padding: 24px; border-radius: 14px; }
.oc-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.oc-id { font-size: 18px; font-weight: 700; color: #fff; }
.oc-price {
  font-size: 18px; font-weight: 700;
  background: linear-gradient(135deg, #ec4899, #a855f7);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
  margin-left: auto;
}
.oc-body { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 16px; }
.oc-info { display: flex; flex-direction: column; gap: 4px; }
.oi-label { font-size: 12px; color: var(--text-4); }
.oi-value { font-size: 14px; color: var(--text-2); }
.oc-actions { display: flex; gap: 8px; }

.quote-content { color: var(--text-2); }

/* 设计稿区:报价的定价依据 */
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
/* 中签角色是实际要生产的,用品牌色描边区分 */
.design-card.selected { border-color: var(--brand-green); box-shadow: 0 0 0 1px var(--brand-green) inset; }
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
