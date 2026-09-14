<template>
  <div class="order-list-page">
    <!-- 页头 -->
    <div class="page-header" v-reveal>
      <div>
        <h1 class="page-title">我的订单</h1>
        <p class="page-desc">查看你的所有定制订单</p>
      </div>
      <el-button round @click="$router.push('/series')">
        <el-icon class="el-icon--left"><Back /></el-icon>
        查看系列
      </el-button>
    </div>

    <!-- 订单卡片 -->
    <div v-loading="loading" class="order-list">
      <div v-if="orders.length === 0 && !loading" class="empty-state glass-card" v-reveal>
        <div class="empty-orb">
          <el-icon :size="48"><Box /></el-icon>
        </div>
        <p>还没有订单</p>
        <el-button round class="btn-liquid" @click="$router.push('/series')">去创建订单</el-button>
      </div>

      <div
        v-for="(order, i) in orders"
        :key="order.id"
        class="order-card glass-card"
        v-reveal="{ index: i }"
        @click="viewDetail(order.id)"
      >
        <div class="order-header">
          <span class="order-id">#{{ order.id }}</span>
          <el-tag :type="getStatusType(order.status)" effect="dark" size="small">
            {{ getStatusLabel(order.status) }}
          </el-tag>
          <el-tag v-if="order.orderType === 'REFILL'" type="warning" effect="dark" size="small">补购</el-tag>
        </div>
        <div class="order-body">
          <div class="order-info-item">
            <span class="info-label">系列</span>
            <span class="info-value">{{ order.seriesId ? getSeriesName(order.seriesId) : '-' }}</span>
          </div>
          <div class="order-info-item">
            <span class="info-label">报价</span>
            <span class="info-value price" v-if="order.quotedPrice">¥{{ order.quotedPrice }}</span>
            <span class="info-value" v-else>-</span>
          </div>
          <div class="order-info-item">
            <span class="info-label">交货日期</span>
            <span class="info-value">{{ order.expectedDeliveryDate || '-' }}</span>
          </div>
          <div class="order-info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ formatDate(order.createdAt) }}</span>
          </div>
        </div>
        <div class="order-footer">
          <span class="view-detail">查看详情 →</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderList } from '@/api/order'
import { getSeriesDetail } from '@/api/series'
import { Back, Box } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const orders = ref([])
const seriesNames = ref({})

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await getOrderList()
    orders.value = res.data || []
    // 批量获取系列名称
    const seriesIds = [...new Set(orders.value.map(o => o.seriesId).filter(Boolean))]
    await Promise.all(seriesIds.map(async (sid) => {
      try {
        const sres = await getSeriesDetail(sid)
        seriesNames.value[sid] = sres.data?.name || `系列 #${sid}`
      } catch {
        seriesNames.value[sid] = `系列 #${sid}`
      }
    }))
  } finally {
    loading.value = false
  }
}

function getSeriesName(seriesId) {
  return seriesNames.value[seriesId] || `系列 #${seriesId}`
}

const getStatusLabel = (status) => {
  const map = {
    'DRAFT_SUBMIT_PENDING': '待提交报价', 'REVIEWING': '终审中', 'REVIEW_REJECTED': '终审拒绝',
    'QUOTED': '已报价', 'CLOSED': '已关闭', 'LOTTERY_PENDING': '待抽奖', 'LOTTERY_DONE': '已抽奖',
    'DEPOSIT_PENDING': '待付定金', 'IN_PRODUCTION': '生产中', 'QC_PENDING': '待质检',
    'BALANCE_PENDING': '待付尾款', 'SHIPPING_PENDING': '待发货', 'SHIPPED': '已发货',
    'COMPLETED': '已完成', 'CANCELLED': '已取消'
  }
  return map[status] || status
}

const getStatusType = (status) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'CANCELLED' || status === 'CLOSED') return 'info'
  if (status === 'REVIEW_REJECTED') return 'warning'
  if (status === 'QUOTED' || status === 'LOTTERY_PENDING') return 'danger'
  return 'primary'
}

const viewDetail = (id) => router.push(`/orders/${id}`)

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleString('zh-CN', { dateStyle: 'short', timeStyle: 'short' })
}

onMounted(loadOrders)
</script>

<style scoped>
.order-list-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 30px;
  font-weight: 700;
  background: var(--gradient-brand);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: gradientShift 6s ease infinite;
}

.page-desc { font-size: 14px; color: var(--text-3); margin-top: 4px; }

.empty-state {
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

.empty-state p { color: var(--text-3); }

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  padding: 24px;
  border-radius: 14px;
  cursor: pointer;
}

.order-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.order-id {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

.order-body {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.order-info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label { font-size: 12px; color: var(--text-4); }
.info-value { font-size: 14px; color: var(--text-2); }
.info-value.price { color: var(--brand-3); font-weight: 600; font-size: 16px; }

.order-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--line-soft);
  display: flex;
  justify-content: flex-end;
}

.view-detail {
  font-size: 14px;
  color: var(--brand-1);
  transition: transform 0.25s var(--ease-out);
}
.order-card:hover .view-detail { transform: translateX(4px); }

@media (max-width: 768px) {
  .order-body { grid-template-columns: repeat(2, 1fr); }
}
</style>
