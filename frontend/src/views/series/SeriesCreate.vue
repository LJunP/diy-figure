<template>
  <div class="series-create-page">
    <div class="page-header" v-reveal>
      <el-button text @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <div class="create-card glass-strong" v-reveal="{ delay: 0.1 }">
      <div class="create-glow"></div>
      <h1 class="form-title">创建系列</h1>
      <p class="form-desc">给你的原创手办系列起个名字,选择规格和尺寸</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" style="max-width: 600px">
        <el-form-item label="系列名称" prop="name">
          <el-input v-model="form.name" placeholder="例如:星空精灵系列" size="large" />
        </el-form-item>

        <el-form-item label="规格档位" prop="specTier">
          <div class="tier-selector">
            <div
              v-for="t in tierOptions"
              :key="t.value"
              class="tier-option glass-card"
              :class="{ active: form.specTier === t.value }"
              @click="form.specTier = t.value"
            >
              <div class="tier-option-name">{{ t.label }}</div>
              <div class="tier-option-detail">
                <span>{{ t.design }} 设计</span>
                <span>{{ t.selected }} 中签</span>
              </div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="尺寸档位" prop="sizeTier">
          <el-radio-group v-model="form.sizeTier" size="large">
            <el-radio-button value="SMALL">小型 (约 6cm)</el-radio-button>
            <el-radio-button value="MEDIUM">中型 (约 10cm)</el-radio-button>
            <el-radio-button value="LARGE">大型 (约 15cm)</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button round size="large" class="btn-liquid" :loading="loading" @click="handleSubmit" style="width: 220px">
            创建系列
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createSeries } from '@/api/series'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const loading = ref(false)

const form = reactive({ name: '', specTier: route.query.tier || 'LIGHT', sizeTier: '' })
const rules = {
  name: [{ required: true, message: '请输入系列名称', trigger: 'blur' }],
  specTier: [{ required: true, message: '请选择规格档位', trigger: 'change' }],
  sizeTier: [{ required: true, message: '请选择尺寸档位', trigger: 'change' }]
}

const tierOptions = [
  { value: 'LIGHT', label: '轻量', design: '6 个', selected: '4 个' },
  { value: 'CLASSIC', label: '经典', design: '9 个', selected: '6 个' },
  { value: 'COLLECTION', label: '收藏', design: '12 个', selected: '8 个' }
]

async function handleSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await createSeries(form)
      ElMessage.success('系列创建成功')
      router.push(`/series/${res.data.id}`)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.series-create-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px;
}

.page-header {
  margin-bottom: 24px;
}

.create-card {
  padding: 44px;
  border-radius: var(--radius-xl);
  position: relative;
  overflow: hidden;
}

.create-glow {
  position: absolute;
  top: -40%; right: -20%;
  width: 60%; height: 80%;
  background: radial-gradient(circle, rgba(109, 124, 255, 0.14), transparent 60%);
  filter: blur(40px);
  pointer-events: none;
}

.form-title {
  font-size: 30px;
  font-weight: 700;
  color: #fff;
  background: var(--gradient-brand);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: gradientShift 6s ease infinite;
  margin-bottom: 8px;
  position: relative;
  z-index: 1;
}

.form-desc {
  font-size: 14px;
  color: var(--text-3);
  margin-bottom: 32px;
  position: relative;
  z-index: 1;
}

.tier-selector {
  display: flex;
  gap: 12px;
  width: 100%;
  position: relative;
  z-index: 1;
}

.tier-option {
  flex: 1;
  padding: 20px;
  border-radius: 14px;
  cursor: pointer;
  text-align: center;
}

.tier-option.active {
  border-color: rgba(109, 124, 255, 0.5) !important;
  background: rgba(109, 124, 255, 0.1) !important;
  box-shadow: 0 0 24px rgba(109, 124, 255, 0.15), var(--glass-highlight);
}

.tier-option-name {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
}

.tier-option.active .tier-option-name {
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.tier-option-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: var(--text-3);
}
</style>
