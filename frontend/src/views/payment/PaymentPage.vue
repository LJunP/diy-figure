<template>
  <div class="payment-page">
    <div class="back-row">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回订单
      </el-button>
    </div>

    <div v-loading="loading" class="payment-container">
      <!-- 支付信息 -->
      <div v-if="payment" class="payment-card glass">
        <div class="pay-header">
          <h2 class="pay-title">支付 - 订单 #{{ orderId }}</h2>
          <el-tag :type="payment.status === 'SUCCESS' ? 'success' : payment.status === 'PENDING' ? 'warning' : 'danger'" effect="dark" size="large">
            {{ getStatusLabel(payment.status) }}
          </el-tag>
        </div>

        <div class="pay-info-grid">
          <div class="pay-info-item">
            <span class="pi-label">支付单号</span>
            <span class="pi-value">{{ payment.paymentId }}</span>
          </div>
          <div class="pay-info-item">
            <span class="pi-label">支付类型</span>
            <span class="pi-value">
              <el-tag :type="payment.type === 'DEPOSIT' ? 'primary' : 'success'" effect="dark" size="small">
                {{ payment.type === 'DEPOSIT' ? '定金(50%)' : '尾款(50%)' }}
              </el-tag>
            </span>
          </div>
          <div class="pay-info-item">
            <span class="pi-label">支付渠道</span>
            <span class="pi-value">
              <el-tag effect="dark" size="small">{{ payment.channel === 'WECHAT' ? '微信支付' : '支付宝' }}</el-tag>
            </span>
          </div>
          <div class="pay-info-item">
            <span class="pi-label">支付金额</span>
            <span class="pi-value amount">¥{{ payment.amount }}</span>
          </div>
          <div class="pay-info-item">
            <span class="pi-label">创建时间</span>
            <span class="pi-value">{{ payment.createdAt }}</span>
          </div>
        </div>

        <!-- 模拟支付区域 -->
        <div v-if="payment.status === 'PENDING' && payment.mockMode" class="mock-pay-section">
          <el-alert title="开发环境模拟支付" type="warning" :closable="false" show-icon>
            当前为开发环境,点击下方按钮模拟支付成功。生产环境将跳转至真实支付网关。
          </el-alert>

          <div class="qr-section">
            <div class="qr-placeholder glass">
              <el-icon :size="64" color="#667eea"><Wallet /></el-icon>
              <p>{{ payment.channel === 'WECHAT' ? '微信支付' : '支付宝' }}</p>
              <p class="qr-amount">¥{{ payment.amount }}</p>
            </div>
            <el-button type="primary" size="large" round :loading="paying" @click="handleSimulatePay" class="pay-btn">
              模拟支付成功
            </el-button>
          </div>
        </div>

        <!-- 支付成功 -->
        <div v-if="payment.status === 'SUCCESS'" class="success-section">
          <el-result icon="success" title="支付成功" :sub-title="`${payment.type === 'DEPOSIT' ? '定金' : '尾款'}支付成功,¥${payment.amount}`">
            <template #extra>
              <el-button type="primary" round @click="goBack">返回订单</el-button>
            </template>
          </el-result>
        </div>
      </div>

      <!-- 选择支付方式 -->
      <div v-if="!payment" class="payment-card glass">
        <h2 class="pay-title">选择支付方式</h2>

        <div class="channel-section">
          <span class="section-label">支付渠道</span>
          <div class="channel-cards">
            <div class="channel-card" :class="{ active: selectedChannel === 'WECHAT' }" @click="selectedChannel = 'WECHAT'">
              <el-icon :size="24" color="#43e97b"><ChatDotRound /></el-icon>
              <span>微信支付</span>
            </div>
            <div class="channel-card" :class="{ active: selectedChannel === 'ALIPAY' }" @click="selectedChannel = 'ALIPAY'">
              <el-icon :size="24" color="#4facfe"><Money /></el-icon>
              <span>支付宝</span>
            </div>
          </div>
        </div>

        <div class="channel-section">
          <span class="section-label">支付类型</span>
          <div class="type-display">
            <div v-if="orderStatus === 'DEPOSIT_PENDING'" class="type-card active">
              <span>定金支付</span>
              <span class="type-amount">¥{{ depositAmount }}</span>
            </div>
            <div v-if="orderStatus === 'BALANCE_PENDING'" class="type-card active">
              <span>尾款支付</span>
              <span class="type-amount">¥{{ balanceAmount }}</span>
            </div>
          </div>
        </div>

        <el-button type="primary" size="large" round :loading="creating" @click="handleCreatePayment" class="create-btn">
          创建支付订单
        </el-button>
      </div>

      <!-- 支付历史 -->
      <div v-if="paymentHistory.length > 0" class="history-card glass">
        <h3 class="section-title">支付记录</h3>
        <el-table :data="paymentHistory" size="small">
          <el-table-column prop="id" label="支付单号" width="80" />
          <el-table-column label="类型" width="80">
            <template #default="{ row }">{{ row.type === 'DEPOSIT' ? '定金' : '尾款' }}</template>
          </el-table-column>
          <el-table-column label="渠道" width="80">
            <template #default="{ row }">{{ row.channel === 'WECHAT' ? '微信' : '支付宝' }}</template>
          </el-table-column>
          <el-table-column label="金额" width="100">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'PENDING' ? 'warning' : 'danger'" size="small">
                {{ getStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="paidAt" label="支付时间" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail } from '@/api/order'
import {
  createDepositPayment,
  createBalancePayment,
  getPaymentHistory,
  simulatePaymentSuccess
} from '@/api/payment'
import { ElMessage } from 'element-plus'
import { Wallet, ChatDotRound, Money, ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const creating = ref(false)
const paying = ref(false)
const orderDetail = ref({})
const payment = ref(null)
const paymentHistory = ref([])
const selectedChannel = ref('WECHAT')
const paymentType = ref('DEPOSIT')

const orderId = computed(() => route.params.orderId)
const orderStatus = computed(() => orderDetail.value.status)
const depositAmount = computed(() => orderDetail.value.depositAmount || '0.00')
const balanceAmount = computed(() => orderDetail.value.balanceAmount || '0.00')

const loadOrderDetail = async () => {
  try {
    const res = await getOrderDetail(orderId.value)
    orderDetail.value = res.data
    if (orderDetail.value.status === 'DEPOSIT_PENDING') paymentType.value = 'DEPOSIT'
    else if (orderDetail.value.status === 'BALANCE_PENDING') paymentType.value = 'BALANCE'
  } catch (error) {
    ElMessage.error('加载订单详情失败')
  }
}

const loadPaymentHistory = async () => {
  try {
    const res = await getPaymentHistory(orderId.value)
    paymentHistory.value = res.data || []
  } catch (error) {
    console.error('加载支付记录失败:', error)
  }
}

const handleCreatePayment = async () => {
  creating.value = true
  try {
    let res
    if (paymentType.value === 'DEPOSIT') res = await createDepositPayment(orderId.value, selectedChannel.value)
    else res = await createBalancePayment(orderId.value, selectedChannel.value)
    payment.value = res.data
    ElMessage.success('支付订单已创建')
  } catch (error) {
    ElMessage.error('创建支付订单失败: ' + (error.response?.data?.message || error.message))
  } finally {
    creating.value = false
  }
}

const handleSimulatePay = async () => {
  paying.value = true
  try {
    const res = await simulatePaymentSuccess(payment.value.paymentId)
    payment.value = res.data
    ElMessage.success('支付成功!')
    await loadOrderDetail()
    await loadPaymentHistory()
  } catch (error) {
    ElMessage.error('支付失败: ' + (error.response?.data?.message || error.message))
  } finally {
    paying.value = false
  }
}

const getStatusLabel = (status) => {
  const map = { PENDING: '待支付', SUCCESS: '已支付', FAILED: '已失败' }
  return map[status] || status
}

const goBack = () => router.push(`/orders/${orderId.value}`)

onMounted(async () => {
  loading.value = true
  await Promise.all([loadOrderDetail(), loadPaymentHistory()])
  loading.value = false
})
</script>

<style scoped>
.payment-page { max-width: 800px; margin: 0 auto; padding: 32px; }
.back-row { margin-bottom: 16px; }

.payment-card { padding: 32px; border-radius: 16px; margin-bottom: 20px; }
.pay-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.pay-title { font-size: 22px; font-weight: 700; color: #fff; }

.pay-info-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.pay-info-item { display: flex; flex-direction: column; gap: 4px; }
.pi-label { font-size: 12px; color: #666; }
.pi-value { font-size: 15px; color: #ccc; }
.pi-value.amount { font-size: 24px; font-weight: 800; color: #f5576c; }

.mock-pay-section { margin-top: 24px; }
.qr-section { display: flex; flex-direction: column; align-items: center; padding: 32px 0; gap: 24px; }
.qr-placeholder { width: 200px; height: 200px; border-radius: 16px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; border: 2px dashed rgba(255,255,255,0.15); }
.qr-placeholder p { color: #888; font-size: 14px; }
.qr-amount { font-size: 24px !important; font-weight: 800; color: #f5576c !important; }
.pay-btn { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border: none; min-width: 200px; }

.success-section { margin-top: 24px; }

.channel-section { margin-bottom: 24px; }
.section-label { font-size: 12px; font-weight: 600; letter-spacing: 2px; color: #667eea; margin-bottom: 12px; display: block; }
.channel-cards { display: flex; gap: 16px; }
.channel-card { display: flex; align-items: center; gap: 8px; padding: 16px 24px; border: 2px solid rgba(255,255,255,0.08); border-radius: 12px; cursor: pointer; transition: all 0.2s; color: #ccc; font-size: 15px; }
.channel-card:hover { border-color: rgba(102,126,234,0.3); background: rgba(255,255,255,0.03); }
.channel-card.active { border-color: #667eea; background: rgba(102,126,234,0.08); color: #fff; }

.type-display { display: flex; gap: 12px; }
.type-card { display: flex; align-items: center; justify-content: space-between; padding: 16px 24px; border: 2px solid #667eea; border-radius: 12px; background: rgba(102,126,234,0.08); color: #fff; width: 100%; }
.type-amount { font-size: 20px; font-weight: 800; color: #f5576c; }

.create-btn { width: 100%; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border: none; }

.history-card { padding: 24px; border-radius: 16px; }
.section-title { font-size: 16px; font-weight: 600; color: #fff; margin-bottom: 16px; }
</style>
