<template>
  <div class="notification-page" v-loading="loading">
    <div class="page-head glass-strong" v-reveal>
      <div>
        <h2 class="page-title">消息中心</h2>
        <p class="page-sub">订单状态变化会在这里通知你</p>
      </div>
      <el-button v-if="unread > 0" round size="large" @click="handleMarkAllRead">
        全部标为已读（{{ unread }}）
      </el-button>
    </div>

    <div class="notification-list glass-card" v-reveal="{ delay: 0.05 }">
      <el-empty v-if="!notifications.length" description="暂无消息" />

      <div
        v-for="item in notifications"
        :key="item.id"
        class="notice-item"
        :class="{ unread: !item.readAt }"
        @click="handleClick(item)"
      >
        <span class="notice-dot" v-if="!item.readAt"></span>
        <div class="notice-body">
          <p class="notice-content">{{ item.content }}</p>
          <div class="notice-meta">
            <span v-if="item.orderId" class="notice-order" @click.stop="goOrder(item.orderId)">
              订单 #{{ item.orderId }}
            </span>
            <span class="notice-time">{{ formatTime(item.createdAt) }}</span>
            <el-tag v-if="!item.readAt" type="danger" size="small" effect="plain">未读</el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listNotifications, getUnreadCount, markNotificationRead, markAllNotificationsRead } from '@/api/notification'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const notifications = ref([])
const unread = ref(0)

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

const loadData = async () => {
  loading.value = true
  try {
    const [listRes, countRes] = await Promise.all([listNotifications(), getUnreadCount()])
    notifications.value = listRes.data || []
    unread.value = countRes.data?.unread ?? 0
  } finally {
    loading.value = false
  }
}

const handleRead = async (item) => {
  if (item.readAt) return
  try {
    await markNotificationRead(item.id)
    item.readAt = new Date().toISOString()
    unread.value = Math.max(0, unread.value - 1)
  } catch (e) {
    ElMessage.error('标记失败')
  }
}

const handleClick = async (item) => {
  await handleRead(item)
  if (item.orderId) goOrder(item.orderId)
}

const handleMarkAllRead = async () => {
  try {
    const res = await markAllNotificationsRead()
    ElMessage.success(`已标记 ${res.data?.updated ?? 0} 条为已读`)
    await loadData()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const goOrder = (orderId) => router.push(`/orders/${orderId}`)

onMounted(loadData)
</script>

<style scoped>
.notification-page { max-width: 900px; margin: 0 auto; padding: 32px; }
.page-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 24px 28px; border-radius: var(--radius-lg); margin-bottom: 20px;
}
.page-title { margin: 0; font-size: 22px; font-weight: 600; color: var(--text-1); }
.page-sub { margin: 6px 0 0; font-size: 13px; color: var(--text-2); }

.notification-list { padding: 8px 8px 16px; border-radius: var(--radius-lg); }
.notice-item {
  position: relative; display: flex; gap: 12px;
  padding: 16px 18px; border-radius: var(--radius-md); cursor: pointer;
  border-bottom: 1px solid var(--line-soft);
  transition: background 0.2s var(--ease-out);
}
.notice-item:last-child { border-bottom: none; }
.notice-item:hover { background: rgba(255, 255, 255, 0.04); }
.notice-item.unread { background: var(--glass-bg-soft, rgba(255, 255, 255, 0.03)); }
.notice-dot {
  flex: none; width: 8px; height: 8px; margin-top: 7px;
  border-radius: 50%; background: var(--el-color-danger);
}
.notice-body { flex: 1; min-width: 0; }
.notice-content { margin: 0 0 8px; font-size: 14px; line-height: 1.6; color: var(--text-1); }
.notice-meta { display: flex; align-items: center; gap: 12px; font-size: 12px; color: var(--text-3); }
.notice-order { color: var(--brand-cyan); cursor: pointer; }
.notice-order:hover { text-decoration: underline; }
</style>
