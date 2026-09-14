<template>
  <div class="profile-page">
    <div class="profile-header glass-strong" v-reveal>
      <div class="header-glow"></div>
      <div class="avatar-lg">{{ userStore.username.charAt(0).toUpperCase() }}</div>
      <div>
        <h1 class="profile-name">{{ userStore.username }}</h1>
        <p class="profile-email">{{ userInfo.email || '-' }}</p>
        <el-tag :type="userStore.isAdmin ? 'danger' : 'primary'" effect="dark" size="small">
          {{ userStore.isAdmin ? '运营管理员' : '普通用户' }}
        </el-tag>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="profile-tabs">
      <!-- 个人信息 -->
      <el-tab-pane label="个人信息" name="info">
        <div class="tab-card glass-card" v-reveal>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">{{ userStore.username }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">
              {{ userInfo.email || '-' }}
              <el-tag v-if="userInfo.emailVerified" type="success" size="small" effect="dark" style="margin-left:8px">已验证</el-tag>
              <el-tag v-else type="warning" size="small" effect="dark" style="margin-left:8px">未验证</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="手机号">{{ userInfo.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-tag :type="userStore.isAdmin ? 'danger' : 'primary'" effect="dark">
                {{ userStore.isAdmin ? '运营管理员' : '普通用户' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <el-alert
            v-if="userInfo.email && userInfo.emailVerified === false"
            title="邮箱尚未验证。验证后可用于找回密码。"
            type="warning"
            show-icon
            :closable="false"
            style="margin: 16px 0"
          >
            <el-button size="small" @click="handleResend">重新发送验证邮件</el-button>
          </el-alert>

          <el-divider>修改密码</el-divider>

          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="80px" style="max-width: 400px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" round :loading="passwordLoading" @click="handleUpdatePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 收货地址 -->
      <el-tab-pane label="收货地址" name="address">
        <div class="tab-card glass-card" v-reveal>
          <div class="addr-header">
            <span>我的收货地址</span>
            <el-button type="primary" round :icon="Plus" @click="openAddressDialog()">新增地址</el-button>
          </div>
          <div v-loading="addressLoading">
            <div v-if="addresses.length === 0" class="empty-addr">
              <p>暂无收货地址</p>
            </div>
            <div v-else class="addr-list">
              <div v-for="addr in addresses" :key="addr.id" class="addr-item">
                <div class="addr-info">
                  <div class="addr-name">
                    <span class="receiver">{{ addr.receiverName }}</span>
                    <span class="phone">{{ addr.phone }}</span>
                  </div>
                  <div class="addr-detail">{{ addr.detail }}</div>
                </div>
                <div class="addr-actions">
                  <el-button text :icon="Edit" @click="openAddressDialog(addr)">编辑</el-button>
                  <el-button text type="danger" :icon="Delete" @click="handleDeleteAddress(addr)">删除</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 快捷入口 -->
      <el-tab-pane label="我的系列" name="series">
        <div class="tab-card glass-card quick-links" v-reveal>
          <el-button type="primary" round size="large" @click="$router.push('/series')">查看我的系列</el-button>
          <el-button round size="large" @click="$router.push('/series/create')">创建新系列</el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的订单" name="orders">
        <div class="tab-card glass-card quick-links" v-reveal>
          <el-button type="primary" round size="large" @click="$router.push('/orders')">查看我的订单</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 地址弹窗 -->
    <el-dialog v-model="addressDialogVisible" :title="editingAddress ? '编辑地址' : '新增地址'" width="460px">
      <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-width="70px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="addressForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addressForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="地址" prop="detail">
          <el-input v-model="addressForm.detail" type="textarea" :rows="3" placeholder="请输入详细收货地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addressSaving" @click="handleSaveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserInfo, updatePassword, resendVerification } from '@/api/auth'
import { listAddresses, createAddress, updateAddress, deleteAddress } from '@/api/address'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

const userStore = useUserStore()
const route = useRoute()
const activeTab = ref(typeof route.query.tab === 'string' ? route.query.tab : 'info')
watch(() => route.query.tab, (tab) => {
  if (typeof tab === 'string' && tab) activeTab.value = tab
})
const userInfo = ref({})

const passwordFormRef = ref()
const passwordLoading = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码至少 6 位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, {
    validator: (rule, value, callback) => {
      if (value !== passwordForm.newPassword) callback(new Error('两次输入的密码不一致'))
      else callback()
    }, trigger: 'blur'
  }]
}

async function handleUpdatePassword() {
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return
    passwordLoading.value = true
    try {
      await updatePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
      ElMessage.success('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    } catch (e) {} finally { passwordLoading.value = false }
  })
}

const addressLoading = ref(false)
const addresses = ref([])
const addressDialogVisible = ref(false)
const addressSaving = ref(false)
const addressFormRef = ref()
const editingAddress = ref(null)
const addressForm = reactive({ receiverName: '', phone: '', detail: '' })
const addressRules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

async function loadAddresses() {
  addressLoading.value = true
  try {
    const res = await listAddresses()
    addresses.value = res.data || []
  } catch (e) {} finally { addressLoading.value = false }
}

function openAddressDialog(addr = null) {
  editingAddress.value = addr
  if (addr) {
    addressForm.receiverName = addr.receiverName
    addressForm.phone = addr.phone
    addressForm.detail = addr.detail
  } else {
    addressForm.receiverName = ''
    addressForm.phone = ''
    addressForm.detail = ''
  }
  addressDialogVisible.value = true
}

async function handleSaveAddress() {
  await addressFormRef.value.validate(async (valid) => {
    if (!valid) return
    addressSaving.value = true
    try {
      const data = { receiverName: addressForm.receiverName, phone: addressForm.phone, detail: addressForm.detail }
      if (editingAddress.value) {
        await updateAddress(editingAddress.value.id, data)
        ElMessage.success('地址修改成功')
      } else {
        await createAddress(data)
        ElMessage.success('地址添加成功')
      }
      addressDialogVisible.value = false
      loadAddresses()
    } catch (e) {} finally { addressSaving.value = false }
  })
}

async function handleDeleteAddress(addr) {
  try {
    await ElMessageBox.confirm('确定要删除此收货地址吗?', '提示', { type: 'warning' })
    await deleteAddress(addr.id)
    ElMessage.success('删除成功')
    loadAddresses()
  } catch (e) {}
}

async function handleResend() {
  if (!userInfo.value.email) return
  try {
    await resendVerification(userInfo.value.email)
    ElMessage.success('如果 SMTP 已配置,验证邮件已发出;否则请查看后端日志')
  } catch (e) {}
}

onMounted(async () => {
  try {
    const res = await getUserInfo()
    userInfo.value = res.data
    userStore.userInfo = res.data
    localStorage.setItem('userInfo', JSON.stringify(res.data))
  } catch (e) {}
  loadAddresses()
})
</script>

<style scoped>
.profile-page { max-width: 800px; margin: 0 auto; padding: 32px; }

.profile-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 32px;
  border-radius: var(--radius-lg);
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
}

.header-glow {
  position: absolute;
  top: -50%; right: -10%;
  width: 50%; height: 80%;
  background: radial-gradient(circle, rgba(109, 124, 255, 0.14), transparent 60%);
  filter: blur(40px);
  pointer-events: none;
}

.avatar-lg {
  width: 68px; height: 68px;
  border-radius: 50%;
  background: var(--gradient-brand);
  background-size: 200% 200%;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 700; color: #fff;
  box-shadow: 0 0 24px rgba(109, 124, 255, 0.4);
  animation: gradientShift 6s ease infinite;
  position: relative;
}

.profile-name { font-size: 22px; font-weight: 700; color: #fff; margin-bottom: 4px; position: relative; }
.profile-email { font-size: 14px; color: var(--text-3); margin-bottom: 8px; position: relative; }

.profile-tabs {
  background: rgba(255, 255, 255, 0.02);
  backdrop-filter: blur(12px);
  border-radius: var(--radius-lg);
  padding: 0 24px;
  border: 1px solid var(--line);
}

.tab-card { padding: 24px; border-radius: 14px; margin-top: 20px; }

.addr-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; font-size: 16px; font-weight: 600; color: #fff; }

.empty-addr { text-align: center; padding: 40px; color: var(--text-4); }

.addr-list { display: flex; flex-direction: column; gap: 12px; }
.addr-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 12px;
  transition: all 0.25s var(--ease-out);
}
.addr-item:hover {
  border-color: rgba(109, 124, 255, 0.35);
  background: rgba(109, 124, 255, 0.04);
}
.addr-name { display: flex; gap: 12px; margin-bottom: 4px; }
.receiver { font-weight: 600; color: #fff; }
.phone { color: var(--text-3); }
.addr-detail { color: var(--text-3); font-size: 14px; }
.addr-actions { display: flex; gap: 4px; flex-shrink: 0; }

.quick-links { display: flex; gap: 12px; justify-content: center; padding: 40px; }
</style>
