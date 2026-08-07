<template>
  <div class="order-detail-page" v-loading="loading">
    <!-- 返回 -->
    <div class="back-row">
      <el-button text @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回订单列表
      </el-button>
    </div>

    <!-- 订单概览 -->
    <div class="order-overview glass">
      <div class="overview-left">
        <span class="order-id">订单 #{{ order.id }}</span>
        <el-tag :type="getStatusType(order.status)" effect="dark" size="large">
          {{ getStatusLabel(order.status) }}
        </el-tag>
        <el-tag v-if="order.orderType === 'REFILL'" type="warning" effect="dark" size="small">补购订单</el-tag>
      </div>
      <div class="overview-right">
        <div v-if="order.quotedPrice" class="price-display">
          <span class="price-label">报价</span>
          <span class="price-value">¥{{ order.quotedPrice }}</span>
        </div>
      </div>
    </div>

    <!-- 状态时间线 -->
    <div class="timeline-section glass">
      <h3 class="section-title">订单进度</h3>
      <div class="order-timeline">
        <div v-for="(stage, idx) in timelineStages" :key="idx" class="ot-item" :class="{ active: stage.active, done: stage.done, skipped: stage.skipped }">
          <div class="ot-dot"></div>
          <div class="ot-content">
            <span class="ot-name">{{ stage.name }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 信息卡片 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <div class="info-card glass">
          <h3 class="card-title">订单信息</h3>
          <div class="info-row"><span class="ir-label">系列名称</span><span class="ir-value">{{ order.series?.name || `系列 #${order.seriesId}` }}</span></div>
          <div class="info-row"><span class="ir-label">定金金额</span><span class="ir-value" v-if="order.depositAmount">¥{{ order.depositAmount }}</span><span class="ir-value" v-else>-</span></div>
          <div class="info-row"><span class="ir-label">尾款金额</span><span class="ir-value" v-if="order.balanceAmount">¥{{ order.balanceAmount }}</span><span class="ir-value" v-else>-</span></div>
          <div class="info-row"><span class="ir-label">预计交货</span><span class="ir-value">{{ order.expectedDeliveryDate || '-' }}</span></div>
          <div class="info-row"><span class="ir-label">创建时间</span><span class="ir-value">{{ order.createdAt }}</span></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="info-card glass">
          <h3 class="card-title">物流信息</h3>
          <div class="info-row"><span class="ir-label">物流公司</span><span class="ir-value">{{ order.trackingCompany || '-' }}</span></div>
          <div class="info-row"><span class="ir-label">物流单号</span><span class="ir-value">{{ order.trackingNumber || '-' }}</span></div>
          <div class="info-row"><span class="ir-label">收货地址</span><span class="ir-value" v-if="order.addressId">地址 #{{ order.addressId }}</span><span class="ir-value" v-else>-</span></div>
        </div>
      </el-col>
    </el-row>

    <!-- 操作区 -->
    <div v-if="currentAction" class="action-card glass">
      <el-alert :title="currentAction.title" :type="currentAction.type" :closable="false" show-icon />
      <p v-if="currentAction.desc" class="action-desc">{{ currentAction.desc }}</p>

      <!-- 地址选择器(LOTTERY_DONE 状态) -->
      <div v-if="order.status === 'LOTTERY_DONE'" class="address-selector">
        <el-select v-model="selectedAddressId" placeholder="选择收货地址" size="large" style="width: 100%; margin-bottom: 12px">
          <el-option
            v-for="addr in addresses"
            :key="addr.id"
            :label="`${addr.receiverName} - ${addr.phone} - ${addr.detail}`"
            :value="addr.id"
          />
        </el-select>
        <el-button text @click="$router.push('/profile?tab=address')">管理地址 →</el-button>
      </div>

      <div class="action-buttons">
        <el-button v-for="btn in currentAction.buttons" :key="btn.label" :type="btn.type" :round="true" size="large" :disabled="btn.disabledIf ? btn.disabledIf() : false" @click="btn.action">
          {{ btn.label }}
        </el-button>
      </div>
    </div>

    <!-- 取消订单 -->
    <div v-if="canCancel" class="action-card glass">
      <el-alert title="取消订单" type="error" :closable="false" show-icon />
      <p class="action-desc">取消订单可能产生违约金,具体金额取决于当前订单阶段。</p>
      <div class="action-buttons">
        <el-button type="danger" round size="large" @click="handleCancelOrder">取消订单</el-button>
      </div>
    </div>

    <!-- 抽奖结果 -->
    <div v-if="order.canvases && (order.status === 'LOTTERY_DONE' || order.status === 'DEPOSIT_PENDING' || order.status === 'IN_PRODUCTION' || order.status === 'QC_PENDING' || order.status === 'BALANCE_PENDING' || order.status === 'SHIPPING_PENDING' || order.status === 'SHIPPED' || order.status === 'COMPLETED' || lotteryResult)" class="lottery-section glass">
      <h3 class="section-title">抽奖结果</h3>
      <el-row :gutter="16">
        <el-col :span="12">
          <h4 class="result-title" style="color: #43e97b">中签 ({{ selectedCanvases.length }})</h4>
          <div class="canvas-list">
            <div v-for="c in selectedCanvases" :key="c.canvasId" class="canvas-row">
              <el-image v-if="c.firstConceptImage" :src="c.firstConceptImage" fit="cover" class="canvas-thumb" />
              <div v-else class="canvas-thumb no-img"></div>
              <span>{{ c.name }}</span>
              <el-tag type="success" size="small">中签</el-tag>
            </div>
          </div>
        </el-col>
        <el-col :span="12">
          <h4 class="result-title" style="color: #888">未中签 ({{ notSelectedCanvases.length }})</h4>
          <div class="canvas-list">
            <div v-for="c in notSelectedCanvases" :key="c.canvasId" class="canvas-row">
              <el-image v-if="c.firstConceptImage" :src="c.firstConceptImage" fit="cover" class="canvas-thumb" />
              <div v-else class="canvas-thumb no-img"></div>
              <span>{{ c.name }}</span>
              <el-tag type="info" size="small">未中签</el-tag>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 所有角色 -->
    <div v-if="order.canvases && order.canvases.length" class="all-canvases glass">
      <h3 class="section-title">所有角色 ({{ order.canvases.length }})</h3>
      <div class="canvas-grid">
        <div v-for="c in order.canvases" :key="c.canvasId" class="cc-card">
          <el-image v-if="c.firstConceptImage" :src="c.firstConceptImage" fit="cover" class="cc-img" />
          <div v-else class="cc-img no-img"><el-icon :size="24"><Picture /></el-icon></div>
          <div class="cc-body">
            <span>{{ c.name }}</span>
            <el-tag :type="c.lotteryResult === 'SELECTED' ? 'success' : 'info'" size="small">
              {{ c.lotteryResult === 'SELECTED' ? '中签' : '未中签' }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 订单完成 -->
    <div v-if="order.status === 'COMPLETED'" class="completed-section">
      <el-result icon="success" title="订单已完成" sub-title="感谢您的使用!">
        <template #extra>
          <el-button round @click="$router.push('/orders')">返回订单列表</el-button>
          <el-button v-if="hasNotSelected" type="primary" round @click="$router.push(`/refill/${order.id}`)">补购未中签角色</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, acceptQuote, rejectQuote, drawLottery, bindOrderAddress } from '@/api/order'
import { listAddresses } from '@/api/address'
import { confirmReceived, cancelOrder } from '@/api/production'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Picture } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const order = ref({})
const lotteryResult = ref(null)
const addresses = ref([])
const selectedAddressId = ref(null)

const selectedCanvases = computed(() => order.value.canvases?.filter(c => c.lotteryResult === 'SELECTED') || [])
const notSelectedCanvases = computed(() => order.value.canvases?.filter(c => c.lotteryResult === 'NOT_SELECTED') || [])
const hasNotSelected = computed(() => notSelectedCanvases.value.length > 0)

// 补购订单跳过的状态
const skippedStatuses = new Set(['REVIEWING', 'QUOTED', 'LOTTERY_PENDING', 'LOTTERY_DONE'])
const isRefillOrder = computed(() => order.value.orderType === 'REFILL')

const statusOrder = [
  'REVIEWING', 'QUOTED', 'LOTTERY_PENDING', 'LOTTERY_DONE',
  'DEPOSIT_PENDING', 'IN_PRODUCTION', 'QC_PENDING',
  'BALANCE_PENDING', 'SHIPPING_PENDING', 'SHIPPED', 'COMPLETED'
]

const timelineStages = computed(() => {
  const currentIdx = statusOrder.indexOf(order.value.status)
  return statusOrder.map((status, idx) => ({
    name: getStatusLabel(status),
    active: idx === currentIdx,
    done: idx < currentIdx,
    skipped: isRefillOrder.value && skippedStatuses.has(status)
  }))
})

const canCancel = computed(() => {
  const s = order.value.status
  return ['REVIEWING', 'QUOTED', 'LOTTERY_PENDING', 'LOTTERY_DONE', 'DEPOSIT_PENDING', 'IN_PRODUCTION'].includes(s)
})

const currentAction = computed(() => {
  const s = order.value.status
  if (s === 'QUOTED') return {
    title: '报价已就绪',
    type: 'info',
    desc: `报价金额: ¥${order.value.quotedPrice}${order.value.expectedDeliveryDate ? ` | 预计交货: ${order.value.expectedDeliveryDate}` : ''}`,
    buttons: [
      { label: '接受报价', type: 'primary', action: handleAcceptQuote },
      { label: '拒绝报价', type: 'default', action: handleRejectQuote }
    ]
  }
  if (s === 'LOTTERY_PENDING') return {
    title: '准备抽奖',
    type: 'warning',
    desc: '点击下方按钮执行抽奖,系统将随机抽中指定数量的角色进行生产',
    buttons: [{ label: '执行抽奖', type: 'primary', action: handleDrawLottery }]
  }
  if (s === 'LOTTERY_DONE') return {
    title: '请选择收货地址',
    type: 'warning',
    desc: `填写收货地址后进入定金支付环节。定金金额: ¥${order.value.quotedPrice ? (order.value.quotedPrice / 2).toFixed(2) : '0.00'}`,
    buttons: [
      { label: '确认地址', type: 'primary', action: handleBindAddress, disabledIf: () => !selectedAddressId.value }
    ]
  }
  if (s === 'DEPOSIT_PENDING') return {
    title: '请支付定金',
    type: 'warning',
    desc: `定金金额(50%): ¥${order.value.depositAmount}`,
    buttons: [{ label: '前往支付', type: 'primary', action: () => router.push(`/payment/${order.value.id}`) }]
  }
  if (s === 'BALANCE_PENDING') return {
    title: '请支付尾款',
    type: 'success',
    desc: `尾款金额(50%): ¥${order.value.balanceAmount}`,
    buttons: [{ label: '前往支付', type: 'primary', action: () => router.push(`/payment/${order.value.id}`) }]
  }
  if (s === 'SHIPPED') return {
    title: '已发货',
    type: 'success',
    desc: `${order.value.trackingCompany} | ${order.value.trackingNumber}`,
    buttons: [
      { label: '确认签收', type: 'success', action: handleConfirmReceived },
      ...(hasNotSelected.value ? [{ label: '补购未中签角色', type: 'default', action: () => router.push(`/refill/${order.value.id}`) }] : [])
    ]
  }
  return null
})

const loadOrderDetail = async () => {
  loading.value = true
  try {
    const res = await getOrderDetail(route.params.id)
    order.value = res.data
  } finally {
    loading.value = false
  }
}

const loadAddresses = async () => {
  try {
    const res = await listAddresses()
    addresses.value = res.data || []
  } catch (e) {}
}

const handleAcceptQuote = async () => {
  try {
    await ElMessageBox.confirm('确认接受报价吗?', '确认', { type: 'warning' })
    await acceptQuote(order.value.id)
    ElMessage.success('已接受报价')
    await loadOrderDetail()
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const handleRejectQuote = async () => {
  try {
    await ElMessageBox.confirm('确认拒绝报价吗?', '确认', { type: 'warning' })
    await rejectQuote(order.value.id)
    ElMessage.success('已拒绝报价')
    await loadOrderDetail()
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const handleDrawLottery = async () => {
  try {
    await ElMessageBox.confirm('确认执行抽奖吗?结果不可撤销。', '确认', { type: 'warning' })
    const res = await drawLottery(order.value.id)
    lotteryResult.value = res.data
    ElMessage.success('抽奖完成')
    await loadOrderDetail()
    await loadAddresses()
  } catch (e) { if (e !== 'cancel') ElMessage.error('抽奖失败') }
}

const handleBindAddress = async () => {
  if (!selectedAddressId.value) {
    ElMessage.warning('请先选择收货地址')
    return
  }
  try {
    await ElMessageBox.confirm('确认使用此收货地址吗?', '确认', { type: 'warning' })
    await bindOrderAddress(order.value.id, selectedAddressId.value)
    ElMessage.success('地址绑定成功')
    await loadOrderDetail()
  } catch (e) { if (e !== 'cancel') ElMessage.error('绑定失败') }
}

const handleConfirmReceived = async () => {
  try {
    await ElMessageBox.confirm('确认已收到货物吗?', '确认签收', { type: 'success' })
    await confirmReceived(order.value.id)
    ElMessage.success('签收成功')
    await loadOrderDetail()
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const handleCancelOrder = async () => {
  try {
    await ElMessageBox.confirm('确认取消订单吗?取消后可能产生违约金。', '取消订单', { type: 'warning' })
    const res = await cancelOrder(order.value.id)
    const data = res.data
    let msg = '订单已取消'
    if (data.depositPenaltyAmount > 0) msg += `\n违约金: ¥${data.depositPenaltyAmount}`
    if (data.depositRefundAmount > 0) msg += `\n退还金额: ¥${data.depositRefundAmount}`
    ElMessage.success(msg)
    await loadOrderDetail()
  } catch (e) { if (e !== 'cancel') ElMessage.error('取消失败: ' + (e.response?.data?.message || e.message)) }
}

const getStatusLabel = (status) => {
  const map = { 'DRAFT_SUBMIT_PENDING':'待提交报价','REVIEWING':'终审中','REVIEW_REJECTED':'终审拒绝','QUOTED':'已报价','CLOSED':'已关闭','LOTTERY_PENDING':'待抽奖','LOTTERY_DONE':'已抽奖','DEPOSIT_PENDING':'待付定金','IN_PRODUCTION':'生产中','QC_PENDING':'待质检','BALANCE_PENDING':'待付尾款','SHIPPING_PENDING':'待发货','SHIPPED':'已发货','COMPLETED':'已完成','CANCELLED':'已取消' }
  return map[status] || status
}

const getStatusType = (status) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'CANCELLED' || status === 'CLOSED') return 'info'
  if (status === 'REVIEW_REJECTED') return 'warning'
  if (status === 'QUOTED' || status === 'LOTTERY_PENDING') return 'danger'
  return 'primary'
}

onMounted(() => { loadOrderDetail(); loadAddresses() })
</script>

<style scoped>
.order-detail-page { max-width: 1000px; margin: 0 auto; padding: 32px; }
.back-row { margin-bottom: 16px; }

.order-overview {
  display: flex; align-items: center; justify-content: space-between;
  padding: 24px 32px; border-radius: 16px; margin-bottom: 20px;
}
.overview-left { display: flex; align-items: center; gap: 12px; }
.order-id { font-size: 24px; font-weight: 700; color: #fff; }
.price-display { display: flex; flex-direction: column; align-items: flex-end; }
.price-label { font-size: 13px; color: #888; }
.price-value { font-size: 28px; font-weight: 800; color: #f5576c; }

/* 时间线 */
.timeline-section { padding: 24px 32px; border-radius: 16px; margin-bottom: 20px; }
.section-title { font-size: 16px; font-weight: 600; color: #fff; margin-bottom: 20px; }
.order-timeline { display: flex; gap: 0; overflow-x: auto; padding-bottom: 8px; }
.ot-item { display: flex; flex-direction: column; align-items: center; min-width: 72px; position: relative; }
.ot-item:not(:last-child)::after { content: ''; position: absolute; top: 6px; left: 50%; right: -50%; height: 2px; background: rgba(255,255,255,0.1); }
.ot-item.done:not(:last-child)::after { background: rgba(67,233,123,0.5); }
.ot-item.skipped:not(:last-child)::after { background: rgba(255,255,255,0.05); border-top: 1px dashed rgba(255,255,255,0.1); height: 0; }
.ot-dot { width: 12px; height: 12px; border-radius: 50%; background: rgba(255,255,255,0.1); margin-bottom: 8px; z-index: 1; }
.ot-item.done .ot-dot { background: #43e97b; }
.ot-item.active .ot-dot { background: #667eea; box-shadow: 0 0 0 4px rgba(102,126,234,0.2); }
.ot-item.skipped .ot-dot { background: rgba(255,255,255,0.05); border: 1px dashed rgba(255,255,255,0.15); }
.ot-content { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.ot-name { font-size: 11px; color: #666; white-space: nowrap; }
.ot-item.active .ot-name { color: #667eea; font-weight: 600; }
.ot-item.done .ot-name { color: #43e97b; }
.ot-item.skipped .ot-name { color: #444; }

/* 信息卡片 */
.info-card { padding: 24px; border-radius: 16px; margin-bottom: 20px; }
.card-title { font-size: 16px; font-weight: 600; color: #fff; margin-bottom: 16px; }
.info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid rgba(255,255,255,0.04); }
.info-row:last-child { border-bottom: none; }
.ir-label { font-size: 13px; color: #888; }
.ir-value { font-size: 14px; color: #ccc; }

/* 操作区 */
.action-card { padding: 24px 32px; border-radius: 16px; margin-bottom: 20px; }
.action-desc { color: #ccc; font-size: 14px; margin: 12px 0 16px; }
.address-selector { margin: 12px 0 16px; }
.action-buttons { display: flex; gap: 12px; }

/* 抽奖 */
.lottery-section { padding: 24px 32px; border-radius: 16px; margin-bottom: 20px; }
.result-title { font-size: 14px; margin-bottom: 12px; }
.canvas-list { display: flex; flex-direction: column; gap: 8px; }
.canvas-row { display: flex; align-items: center; gap: 8px; padding: 8px 0; border-bottom: 1px solid rgba(255,255,255,0.04); }
.canvas-thumb { width: 40px; height: 40px; border-radius: 6px; overflow: hidden; }
.canvas-thumb.no-img { background: rgba(255,255,255,0.05); }

/* 所有角色 */
.all-canvases { padding: 24px 32px; border-radius: 16px; }
.canvas-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.cc-card { background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06); border-radius: 10px; overflow: hidden; }
.cc-img { width: 100%; height: 120px; }
.cc-img.no-img { display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.02); color: #333; }
.cc-body { padding: 8px 12px; display: flex; align-items: center; justify-content: space-between; }
.cc-body span { font-size: 13px; color: #ccc; }

.completed-section { margin-top: 20px; }
</style>
