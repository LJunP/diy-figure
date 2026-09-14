<template>
  <div class="refill-page">
    <div class="back-row" v-reveal>
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回订单
      </el-button>
    </div>

    <div v-loading="loading" class="refill-container">
      <!-- 补购说明 -->
      <div class="refill-info glass-strong" v-reveal="{ delay: 0.05 }">
        <div class="info-glow"></div>
        <h2 class="page-title">补购 - 订单 #{{ orderId }}</h2>
        <div class="info-items">
          <div class="info-pill">
            <el-icon color="#6d7cff"><Coin /></el-icon>
            <span>补购定价:(套餐总价 ÷ 中签数) × 1.3</span>
          </div>
          <div class="info-pill">
            <el-icon color="#34d399"><Clock /></el-icon>
            <span>补购窗口:主订单发货后 60 天内有效</span>
          </div>
          <div class="info-pill">
            <el-icon color="#ec4899"><Flag /></el-icon>
            <span>跳过终审/报价/抽奖,直接进入定金支付</span>
          </div>
        </div>
      </div>

      <!-- 可补购角色列表 -->
      <div v-if="refillableCanvases.length > 0">
        <h3 class="section-title" v-reveal>未中签角色 ({{ refillableCanvases.length }})</h3>
        <div class="refill-grid">
          <div
            v-for="(canvas, i) in refillableCanvases"
            :key="canvas.canvasId"
            class="refill-card glass-card"
            :class="{ expired: canvas.expired || canvas.alreadyRefilled }"
            v-reveal="{ index: i }"
          >
            <div class="rc-visual">
              <el-image v-if="canvas.firstConceptImage" :src="canvas.firstConceptImage" fit="cover" class="rc-img" />
              <div v-else class="rc-img no-img"><el-icon :size="32" color="#3a3a48"><Picture /></el-icon></div>
              <div class="rc-status" :class="(canvas.alreadyRefilled || canvas.expired) ? 'expired-badge' : 'available-badge'">
                {{ canvas.alreadyRefilled ? '已补购' : (canvas.expired ? '已过期' : '可补购') }}
              </div>
            </div>
            <div class="rc-body">
              <h4 class="rc-name">{{ canvas.canvasName }}</h4>
              <p class="rc-price">¥{{ canvas.refillPrice }}</p>
              <p v-if="canvas.refillAvailableUntil" class="rc-deadline">
                <el-icon><Clock /></el-icon>
                {{ canvas.refillAvailableUntil }}
              </p>
              <el-button v-if="!canvas.expired && !canvas.alreadyRefilled" type="primary" round size="small" :loading="refilling === canvas.canvasId" @click="handleRefill(canvas)" class="rc-btn">
                发起补购
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="!loading" class="empty glass-card" v-reveal>
        <div class="empty-orb"><el-icon :size="48"><Box /></el-icon></div>
        <p>暂无可补购角色</p>
      </div>

      <!-- 补购订单列表 -->
      <div v-if="refillOrders.length > 0" class="refill-orders glass-card" v-reveal>
        <h3 class="section-title">我的补购订单</h3>
        <el-table :data="refillOrders" stripe>
          <el-table-column prop="id" label="补购订单 ID" width="100" />
          <el-table-column prop="parentOrderId" label="主订单 ID" width="100" />
          <el-table-column label="报价" width="100">
            <template #default="{ row }">¥{{ row.quotedPrice }}</template>
          </el-table-column>
          <el-table-column label="定金" width="100">
            <template #default="{ row }">¥{{ row.depositAmount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" effect="dark" size="small">{{ getStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" />
          <el-table-column label="操作" fixed="right" width="100">
            <template #default="{ row }">
              <el-button type="primary" text size="small" @click="$router.push(`/orders/${row.id}`)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 补购确认弹窗 -->
    <el-dialog v-model="showConfirmDialog" title="确认补购" width="480px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="角色">{{ selectedCanvas?.canvasName }}</el-descriptions-item>
        <el-descriptions-item label="补购价格">¥{{ selectedCanvas?.refillPrice }}</el-descriptions-item>
        <el-descriptions-item label="定金(50%)">¥{{ (selectedCanvas?.refillPrice / 2).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="尾款(50%)">¥{{ (selectedCanvas?.refillPrice / 2).toFixed(2) }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-position="top" style="margin-top: 16px">
        <el-form-item label="支付渠道">
          <el-radio-group v-model="selectedChannel">
            <el-radio value="WECHAT">微信支付</el-radio>
            <el-radio value="ALIPAY">支付宝</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showConfirmDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmRefill">确认补购</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRefillableCanvases, createRefillOrder, getRefillOrders } from '@/api/refill'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Picture, Box, Coin, Clock, Flag } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const refillableCanvases = ref([])
const refillOrders = ref([])
const showConfirmDialog = ref(false)
const selectedCanvas = ref(null)
const selectedChannel = ref('WECHAT')
const refilling = ref(null)

const orderId = route.params.orderId

const loadData = async () => {
  loading.value = true
  try {
    const [canvasesRes, ordersRes] = await Promise.all([
      getRefillableCanvases(orderId),
      getRefillOrders()
    ])
    refillableCanvases.value = canvasesRes.data || []
    refillOrders.value = ordersRes.data || []
  } catch (error) {
    ElMessage.error('加载失败: ' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}

const handleRefill = (canvas) => {
  selectedCanvas.value = canvas
  selectedChannel.value = 'WECHAT'
  showConfirmDialog.value = true
}

const confirmRefill = async () => {
  submitting.value = true
  refilling.value = selectedCanvas.value.canvasId
  try {
    const res = await createRefillOrder(orderId, {
      canvasId: selectedCanvas.value.canvasId,
      channel: selectedChannel.value
    })
    ElMessage.success('补购订单已创建')
    showConfirmDialog.value = false
    router.push(`/payment/${res.data.refillOrderId}`)
  } catch (error) {
    ElMessage.error('补购失败: ' + (error.response?.data?.message || error.message))
  } finally {
    submitting.value = false
    refilling.value = null
  }
}

const getStatusLabel = (status) => {
  const map = {
    'DEPOSIT_PENDING': '待付定金', 'IN_PRODUCTION': '生产中', 'QC_PENDING': '待质检',
    'BALANCE_PENDING': '待付尾款', 'SHIPPING_PENDING': '待发货', 'SHIPPED': '已发货',
    'COMPLETED': '已完成', 'CANCELLED': '已取消'
  }
  return map[status] || status
}

const getStatusType = (status) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'CANCELLED') return 'info'
  if (status === 'DEPOSIT_PENDING' || status === 'BALANCE_PENDING') return 'danger'
  return 'primary'
}

const goBack = () => router.push(`/orders/${orderId}`)

onMounted(loadData)
</script>

<style scoped>
.refill-page { max-width: 1000px; margin: 0 auto; padding: 32px; }
.back-row { margin-bottom: 16px; }

.refill-info { padding: 28px 32px; border-radius: var(--radius-lg); margin-bottom: 28px; position: relative; overflow: hidden; }

.info-glow {
  position: absolute;
  top: -40%; right: -10%;
  width: 50%; height: 80%;
  background: radial-gradient(circle, rgba(109, 124, 255, 0.12), transparent 60%);
  filter: blur(40px);
  pointer-events: none;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  background: var(--gradient-brand);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: gradientShift 6s ease infinite;
  margin-bottom: 16px;
  position: relative;
}
.info-items { display: flex; flex-direction: column; gap: 8px; position: relative; }
.info-pill { display: flex; align-items: center; gap: 8px; color: var(--text-2); font-size: 14px; }

.section-title { font-size: 18px; font-weight: 600; color: #fff; margin-bottom: 16px; }

.refill-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.refill-card { border-radius: 14px; overflow: hidden; }
.refill-card.expired { opacity: 0.4; }

.rc-visual { position: relative; height: 160px; overflow: hidden; }
.rc-img { width: 100%; height: 100%; }
.rc-img.no-img { display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.02); }
.rc-status { position: absolute; top: 8px; left: 8px; padding: 3px 10px; border-radius: 6px; font-size: 11px; font-weight: 600; backdrop-filter: blur(8px); }
.available-badge { background: rgba(52, 211, 153, 0.85); color: #fff; }
.expired-badge { background: rgba(255,255,255,0.2); color: var(--text-2); }

.rc-body { padding: 16px; text-align: center; }
.rc-name { font-size: 15px; font-weight: 600; color: #fff; margin-bottom: 8px; }
.rc-price {
  font-size: 20px;
  font-weight: 800;
  background: linear-gradient(135deg, #ec4899, #a855f7);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}
.rc-deadline { display: flex; align-items: center; justify-content: center; gap: 4px; font-size: 12px; color: var(--text-4); margin-bottom: 12px; }
.rc-btn { width: 100%; }

.empty { display: flex; flex-direction: column; align-items: center; padding: 60px; border-radius: var(--radius-lg); gap: 16px; }
.empty-orb {
  width: 80px; height: 80px;
  border-radius: 50%;
  background: rgba(109, 124, 255, 0.1);
  display: flex; align-items: center; justify-content: center;
  color: var(--brand-1);
  box-shadow: 0 0 32px rgba(109, 124, 255, 0.12);
  animation: float 4s ease-in-out infinite;
}
.empty p { color: var(--text-3); }

.refill-orders { padding: 24px 32px; border-radius: var(--radius-lg); margin-top: 28px; }

@media (max-width: 768px) {
  .refill-grid { grid-template-columns: 1fr; }
}
</style>
