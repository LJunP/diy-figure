<template>
  <div class="review-list-page">
    <!-- 页头 -->
    <div class="page-header">
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
      <div v-if="orders.length === 0 && !loading" class="empty glass">
        <el-icon :size="48" color="#333"><DocumentChecked /></el-icon>
        <p>没有待终审的订单</p>
      </div>

      <div v-for="order in orders" :key="order.id" class="order-card glass">
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
    <el-dialog v-model="reviewDialogVisible" title="终审操作" width="500px">
      <div class="review-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单 ID">{{ currentOrder?.id }}</el-descriptions-item>
          <el-descriptions-item label="用户 ID">{{ currentOrder?.userId }}</el-descriptions-item>
          <el-descriptions-item label="系列 ID">{{ currentOrder?.seriesId }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentOrder?.createdAt }}</el-descriptions-item>
        </el-descriptions>

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
import { getPendingReviews, reviewOrder } from '@/api/admin'
import { ElMessage } from 'element-plus'
import { Refresh, DocumentChecked } from '@element-plus/icons-vue'

const loading = ref(false)
const submitting = ref(false)
const orders = ref([])
const reviewDialogVisible = ref(false)
const currentOrder = ref(null)
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

const openReviewDialog = (order) => {
  currentOrder.value = order
  reviewForm.result = 'APPROVED'
  reviewForm.rejectReason = ''
  reviewDialogVisible.value = true
}

const handleReview = async () => {
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
.page-title { font-size: 22px; font-weight: 700; color: #fff; }
.page-desc { font-size: 14px; color: #666; margin-top: 4px; }

.empty { display: flex; flex-direction: column; align-items: center; padding: 60px; border-radius: 16px; gap: 16px; }
.empty p { color: #666; }

.order-list { display: flex; flex-direction: column; gap: 16px; }

.order-card { padding: 24px; border-radius: 14px; }
.oc-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.oc-id { font-size: 18px; font-weight: 700; color: #fff; }
.oc-body { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 16px; }
.oc-info { display: flex; flex-direction: column; gap: 4px; }
.oi-label { font-size: 12px; color: #666; }
.oi-value { font-size: 14px; color: #ccc; }
.oc-actions { display: flex; gap: 8px; }

.review-content { color: #ccc; }
</style>
