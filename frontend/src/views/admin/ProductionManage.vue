<template>
  <div class="production-page">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 生产中 -->
      <el-tab-pane name="production">
        <template #label><span class="tab-label">生产中</span></template>
        <div v-loading="loading" class="tab-content">
          <div v-if="orders.length === 0 && !loading" class="empty glass-card" v-reveal><p>没有生产中的订单</p></div>
          <div v-for="(order, i) in orders" :key="order.id" class="order-card glass-card" v-reveal="{ index: i }">
            <div class="oc-header">
              <span class="oc-id">#{{ order.id }}</span>
              <span class="oc-price">¥{{ order.quotedPrice }}</span>
            </div>
            <div class="oc-info-row">
              <span>用户 #{{ order.userId }}</span>
              <span v-if="order.productionStartedAt" class="started">已开工 {{ order.productionStartedAt }}</span>
              <el-tag v-else type="warning" effect="dark" size="small">未开工</el-tag>
            </div>
            <div class="oc-actions">
              <el-button v-if="!order.productionStartedAt" type="warning" round size="small" @click="handleMarkStart(order.id)">标记开工</el-button>
              <el-button type="primary" round size="small" @click="handleSubmitQc(order.id)">送检</el-button>
              <el-button round size="small" @click="openQcLogs(order.id)">质检记录</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 待质检 -->
      <el-tab-pane name="qc">
        <template #label><span class="tab-label">待质检</span></template>
        <div v-loading="loading" class="tab-content">
          <div v-if="qcOrders.length === 0 && !loading" class="empty glass-card" v-reveal><p>没有待质检的订单</p></div>
          <div v-for="(order, i) in qcOrders" :key="order.id" class="order-card glass-card" v-reveal="{ index: i }">
            <div class="oc-header"><span class="oc-id">#{{ order.id }}</span><span class="oc-price">¥{{ order.quotedPrice }}</span></div>
            <div class="oc-info-row"><span>用户 #{{ order.userId }}</span></div>
            <div class="oc-actions">
              <el-button type="success" round size="small" @click="openQcDialog(order.id, 'PASSED')">质检通过</el-button>
              <el-button type="danger" round size="small" @click="openQcDialog(order.id, 'FAILED')">不通过</el-button>
              <el-button round size="small" @click="openQcLogs(order.id)">质检记录</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 待发货 -->
      <el-tab-pane name="shipping">
        <template #label><span class="tab-label">待发货</span></template>
        <div v-loading="loading" class="tab-content">
          <div v-if="shippingOrders.length === 0 && !loading" class="empty glass-card" v-reveal><p>没有待发货的订单</p></div>
          <div v-for="(order, i) in shippingOrders" :key="order.id" class="order-card glass-card" v-reveal="{ index: i }">
            <div class="oc-header"><span class="oc-id">#{{ order.id }}</span><span class="oc-price">¥{{ order.balanceAmount }}</span></div>
            <div class="oc-info-row"><span>用户 #{{ order.userId }}</span></div>
            <div class="oc-actions"><el-button type="primary" round size="small" @click="openShipDialog(order.id)">发货</el-button></div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 已发货 -->
      <el-tab-pane name="shipped">
        <template #label><span class="tab-label">已发货</span></template>
        <div v-loading="loading" class="tab-content">
          <div v-if="shippedOrders.length === 0 && !loading" class="empty glass-card" v-reveal><p>没有已发货的订单</p></div>
          <div v-for="(order, i) in shippedOrders" :key="order.id" class="order-card glass-card" v-reveal="{ index: i }">
            <div class="oc-header"><span class="oc-id">#{{ order.id }}</span></div>
            <div class="oc-info-row"><span>{{ order.trackingCompany }} {{ order.trackingNumber }}</span></div>
            <div class="oc-actions"><el-button type="success" round size="small" @click="handleComplete(order.id)">标记完成</el-button></div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 违约记录 -->
      <el-tab-pane name="cancellations">
        <template #label><span class="tab-label">违约记录</span></template>
        <div v-loading="loading" class="tab-content">
          <div v-if="cancellations.length === 0 && !loading" class="empty glass-card" v-reveal><p>没有违约记录</p></div>
          <div v-for="(c, i) in cancellations" :key="c.id" class="order-card glass-card" v-reveal="{ index: i }">
            <div class="oc-header"><span class="oc-id">#{{ c.id }}</span><span class="oc-price">订单 #{{ c.orderId }}</span></div>
            <div class="oc-info-row">
              <span>状态: {{ c.stageAtCancel }}</span>
              <span>退还: ¥{{ c.depositRefundAmount || '0.00' }}</span>
              <span>扣除: ¥{{ c.depositPenaltyAmount || '0.00' }}</span>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 质检不通过弹窗 -->
    <el-dialog v-model="showQcDialog" title="质检不通过" width="480px">
      <el-form label-position="top">
        <el-form-item label="不通过原因"><el-input v-model="qcFailReason" type="textarea" :rows="3" placeholder="请输入不通过原因" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showQcDialog = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="handleQcFailed">确认</el-button>
      </template>
    </el-dialog>

    <!-- 发货弹窗 -->
    <el-dialog v-model="showShipDialog" title="发货" width="480px">
      <el-form :model="shipForm" label-position="top">
        <el-form-item label="物流公司"><el-input v-model="shipForm.trackingCompany" placeholder="如:顺丰速运" /></el-form-item>
        <el-form-item label="物流单号"><el-input v-model="shipForm.trackingNumber" placeholder="请输入物流单号" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showShipDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleShip">确认发货</el-button>
      </template>
    </el-dialog>

    <!-- 质检记录弹窗 -->
    <el-dialog v-model="showQcLogsDialog" :title="`订单 #${qcLogsOrderId} 质检记录`" width="560px">
      <div v-loading="qcLogsLoading" class="qc-logs">
        <p v-if="!qcLogsLoading && qcLogs.length === 0" class="qc-logs-empty">
          还没有质检记录。订单送检并录入结果后,这里会显示每一次质检的时间与结论。
        </p>
        <div v-for="log in qcLogs" :key="log.id" class="qc-log-row">
          <el-tag :type="log.result === 'PASSED' ? 'success' : 'danger'" effect="dark" size="small">
            {{ log.result === 'PASSED' ? '通过' : '不通过' }}
          </el-tag>
          <div class="qc-log-body">
            <span class="qc-log-time">{{ formatTime(log.checkedAt) }}</span>
            <span v-if="log.failReason" class="qc-log-reason">{{ log.failReason }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showQcLogsDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  getProductionList, getQcPendingList, getShippedList, getPendingShipping,
  submitForQc, markProductionStarted,
  qualityCheck, shipOrder, completeOrder, getCancellations, getQcLogs
} from '@/api/production'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const submitting = ref(false)
const activeTab = ref('production')
const orders = ref([])
const qcOrders = ref([])
const shippingOrders = ref([])
const shippedOrders = ref([])
const cancellations = ref([])
const showQcDialog = ref(false)
const showShipDialog = ref(false)
const currentOrderId = ref(null)
const qcFailReason = ref('')
const shipForm = reactive({ trackingCompany: '', trackingNumber: '' })

// 质检记录
const showQcLogsDialog = ref(false)
const qcLogsLoading = ref(false)
const qcLogsOrderId = ref(null)
const qcLogs = ref([])

function formatTime(d) {
  if (!d) return ''
  return new Date(d).toLocaleString('zh-CN', { dateStyle: 'short', timeStyle: 'short' })
}

const openQcLogs = async (orderId) => {
  qcLogsOrderId.value = orderId
  qcLogs.value = []
  showQcLogsDialog.value = true
  qcLogsLoading.value = true
  try {
    const res = await getQcLogs(orderId)
    qcLogs.value = res.data || []
  } finally { qcLogsLoading.value = false }
}

const loadProduction = async () => {
  loading.value = true
  try {
    const res = await getProductionList()
    orders.value = res.data || []
  } finally { loading.value = false }
}

const loadQcPending = async () => {
  loading.value = true
  try {
    const res = await getQcPendingList()
    qcOrders.value = res.data || []
  } finally { loading.value = false }
}

const loadShipping = async () => {
  loading.value = true
  try {
    const res = await getPendingShipping()
    shippingOrders.value = res.data || []
  } finally { loading.value = false }
}

const loadShipped = async () => {
  loading.value = true
  try {
    const res = await getShippedList()
    shippedOrders.value = res.data || []
  } finally { loading.value = false }
}

const loadCancellations = async () => {
  loading.value = true
  try {
    const res = await getCancellations()
    cancellations.value = res.data || []
  } finally { loading.value = false }
}

const handleTabChange = (tab) => {
  if (tab === 'production') loadProduction()
  else if (tab === 'qc') loadQcPending()
  else if (tab === 'shipping') loadShipping()
  else if (tab === 'shipped') loadShipped()
  else if (tab === 'cancellations') loadCancellations()
}

const handleMarkStart = async (orderId) => {
  try {
    await ElMessageBox.confirm('确认标记生产开工吗?', '确认', { type: 'warning' })
    await markProductionStarted(orderId)
    ElMessage.success('已标记开工')
    loadProduction()
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const handleSubmitQc = async (orderId) => {
  try {
    await ElMessageBox.confirm('确认提交质检吗?', '确认', { type: 'warning' })
    await submitForQc(orderId)
    ElMessage.success('已提交质检')
    loadProduction()
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const openQcDialog = (orderId, result) => {
  if (result === 'PASSED') {
    ElMessageBox.confirm('确认质检通过吗?', '质检通过', { type: 'success' })
      .then(async () => {
        await qualityCheck(orderId, { result: 'PASSED' })
        ElMessage.success('质检通过')
        loadQcPending()
      }).catch(() => {})
  } else {
    currentOrderId.value = orderId
    qcFailReason.value = ''
    showQcDialog.value = true
  }
}

const handleQcFailed = async () => {
  if (!qcFailReason.value.trim()) { ElMessage.warning('请输入不通过原因'); return }
  submitting.value = true
  try {
    await qualityCheck(currentOrderId.value, { result: 'FAILED', failReason: qcFailReason.value })
    ElMessage.success('已标记质检不通过')
    showQcDialog.value = false
    loadQcPending()
  } catch (e) { ElMessage.error('操作失败') }
  finally { submitting.value = false }
}

const openShipDialog = (orderId) => {
  currentOrderId.value = orderId
  shipForm.trackingCompany = ''
  shipForm.trackingNumber = ''
  showShipDialog.value = true
}

const handleShip = async () => {
  if (!shipForm.trackingCompany.trim() || !shipForm.trackingNumber.trim()) { ElMessage.warning('请填写完整物流信息'); return }
  submitting.value = true
  try {
    await shipOrder(currentOrderId.value, { ...shipForm })
    ElMessage.success('发货成功')
    showShipDialog.value = false
    loadShipping()
  } catch (e) { ElMessage.error('发货失败') }
  finally { submitting.value = false }
}

const handleComplete = async (orderId) => {
  try {
    await ElMessageBox.confirm('确认标记订单完成吗?', '确认', { type: 'warning' })
    await completeOrder(orderId)
    ElMessage.success('订单已完成')
    loadShipped()
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

onMounted(loadProduction)
</script>

<style scoped>
.production-page { max-width: 1000px; margin: 0 auto; }
.tab-label { font-size: 14px; }
.tab-content { padding-top: 16px; }
.empty { display: flex; align-items: center; justify-content: center; padding: 60px; border-radius: 14px; }
.empty p { color: var(--text-3); }
.order-card { padding: 24px; border-radius: 14px; margin-bottom: 16px; }
.oc-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.oc-id { font-size: 18px; font-weight: 700; color: #fff; }
.oc-price {
  font-size: 16px; font-weight: 600;
  background: linear-gradient(135deg, #ec4899, #a855f7);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.oc-info-row { display: flex; align-items: center; gap: 16px; margin-bottom: 12px; font-size: 13px; color: var(--text-3); }
.oc-info-row .started { color: var(--brand-green); }
.oc-actions { display: flex; gap: 8px; flex-wrap: wrap; }

.qc-logs { min-height: 80px; display: flex; flex-direction: column; gap: 12px; }
.qc-logs-empty { margin: 0; font-size: 13px; color: var(--text-3); line-height: 1.7; }
.qc-log-row { display: flex; align-items: flex-start; gap: 12px; }
.qc-log-body { display: flex; flex-direction: column; gap: 4px; }
.qc-log-time { font-size: 13px; color: var(--text-2); }
.qc-log-reason { font-size: 13px; color: var(--el-color-danger); }
</style>
