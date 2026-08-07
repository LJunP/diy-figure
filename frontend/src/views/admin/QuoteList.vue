<template>
  <div class="quote-list-page">
    <!-- 页头 -->
    <div class="page-header">
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
      <div v-if="orders.length === 0 && !loading" class="empty glass">
        <el-icon :size="48" color="#333"><Money /></el-icon>
        <p>没有待报价的订单</p>
      </div>

      <div v-for="order in orders" :key="order.id" class="order-card glass">
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
    <el-dialog v-model="quoteDialogVisible" title="报价" width="480px">
      <el-form :model="quoteForm" label-position="top">
        <el-form-item label="报价金额 (元)">
          <el-input-number v-model="quoteForm.quotedPrice" :min="0" :precision="2" size="large" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预计交货日期">
          <el-date-picker v-model="quoteForm.expectedDeliveryDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" size="large" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quoteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleQuote">确认报价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getPendingQuotes, quoteOrder } from '@/api/admin'
import { ElMessage } from 'element-plus'
import { Refresh, Money } from '@element-plus/icons-vue'

const loading = ref(false)
const submitting = ref(false)
const orders = ref([])
const quoteDialogVisible = ref(false)
const currentOrderId = ref(null)
const quoteForm = reactive({ quotedPrice: 0, expectedDeliveryDate: '' })

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

const openQuoteDialog = (order) => {
  currentOrderId.value = order.id
  quoteForm.quotedPrice = order.quotedPrice || 0
  quoteForm.expectedDeliveryDate = order.expectedDeliveryDate || ''
  quoteDialogVisible.value = true
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
.page-title { font-size: 22px; font-weight: 700; color: #fff; }
.page-desc { font-size: 14px; color: #666; margin-top: 4px; }

.empty { display: flex; flex-direction: column; align-items: center; padding: 60px; border-radius: 16px; gap: 16px; }
.empty p { color: #666; }

.order-list { display: flex; flex-direction: column; gap: 16px; }
.order-card { padding: 24px; border-radius: 14px; }
.oc-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.oc-id { font-size: 18px; font-weight: 700; color: #fff; }
.oc-price { font-size: 18px; font-weight: 700; color: #f5576c; margin-left: auto; }
.oc-body { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 16px; }
.oc-info { display: flex; flex-direction: column; gap: 4px; }
.oi-label { font-size: 12px; color: #666; }
.oi-value { font-size: 14px; color: #ccc; }
.oc-actions { display: flex; gap: 8px; }
</style>
