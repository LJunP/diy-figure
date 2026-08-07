<template>
  <div class="canvas-design" v-loading="loading">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button text :icon="ArrowLeft" @click="$router.back()">返回系列</el-button>
        <el-divider direction="vertical" />
        <span class="canvas-status">
          <el-tag size="small" :type="canvas.status === 'FINALIZED' ? 'success' : 'info'">
            {{ canvas.status === 'FINALIZED' ? '已定稿' : '设计中' }}
          </el-tag>
          <el-tag v-if="canvas.locked" size="small" type="danger">已锁定</el-tag>
        </span>
      </div>
      <div class="toolbar-right">
        <el-button
          v-if="canvas.status === 'DESIGNING' && !canvas.locked"
          type="success"
          :icon="Check"
          :loading="finalizing"
          @click="handleFinalize"
        >
          定稿
        </el-button>
        <el-button
          v-if="canvas.status === 'FINALIZED' && !canvas.locked"
          type="warning"
          @click="handleReopen"
        >
          重新编辑
        </el-button>
      </div>
    </div>

    <!-- 主体:左右分栏 -->
    <div class="main-content">
      <!-- 左侧:AI 对话窗口 -->
      <div class="chat-panel">
        <div class="chat-header">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 设计助手</span>
        </div>

        <!-- 对话记录 -->
        <div class="chat-messages" ref="messagesRef">
          <!-- 空状态欢迎消息 -->
          <div v-if="chatMessages.length === 0 && !aiTyping" class="chat-welcome">
            <div class="welcome-icon">
              <el-icon :size="40" color="#667eea"><MagicStick /></el-icon>
            </div>
            <h3>AI 设计助手</h3>
            <p>描述你想要的原创角色,我会帮你设计外观、色彩方案和背景故事。</p>
            <div class="welcome-suggestions">
              <div class="suggestion-chip" @click="useSuggestion('设计一个星空精灵,Q版风格,主色调为深蓝和银色,头上有星星装饰')">
                ✨ 设计一个星空精灵
              </div>
              <div class="suggestion-chip" @click="useSuggestion('设计一个赛博朋克风格的机械少女,银色机甲,霓虹蓝紫色调')">
                🤖 赛博朋克机械少女
              </div>
              <div class="suggestion-chip" @click="useSuggestion('设计一个古风仙人,白衣飘逸,手持长剑,仙气环绕')">
                ⚔️ 古风剑仙
              </div>
            </div>
          </div>
          <div
            v-for="(msg, idx) in chatMessages"
            :key="idx"
            :class="['message', msg.role]"
          >
            <div class="message-avatar">
              <el-icon v-if="msg.role === 'user'"><User /></el-icon>
              <el-icon v-else><MagicStick /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-text">{{ msg.content }}</div>
              <div v-if="msg.images && msg.images.length" class="message-images">
                <el-image
                  v-for="(img, i) in msg.images"
                  :key="i"
                  :src="img"
                  :preview-src-list="msg.images"
                  fit="cover"
                  class="msg-img"
                />
              </div>
            </div>
          </div>

          <!-- AI 正在输入 -->
          <div v-if="aiTyping" class="message assistant">
            <div class="message-avatar"><el-icon><MagicStick /></el-icon></div>
            <div class="message-content">
              <div class="message-text typing">{{ typingText }}</div>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="chat-input" v-if="canvas.status === 'DESIGNING' && !canvas.locked">
          <!-- 已上传的参考图预览 -->
          <div v-if="uploadedImages.length" class="uploaded-images">
            <div v-for="(img, i) in uploadedImages" :key="i" class="uploaded-img-wrapper">
              <el-image :src="img" fit="cover" class="uploaded-img" />
              <el-button
                text
                type="danger"
                :icon="Close"
                circle
                size="small"
                class="remove-img-btn"
                @click="uploadedImages.splice(i, 1)"
              />
            </div>
          </div>

          <div class="input-row">
            <el-upload
              :show-file-list="false"
              :before-upload="handleUpload"
              accept="image/*"
            >
              <el-button :icon="Picture" circle />
            </el-upload>

            <el-input
              v-model="inputText"
              type="textarea"
              :rows="2"
              placeholder="描述你想要的角色设计..."
              @keydown.enter.exact.prevent="handleSend"
            />

            <el-button
              type="primary"
              :icon="Promotion"
              :loading="sending"
              @click="handleSend"
            >
              发送
            </el-button>
          </div>

          <div class="input-options">
            <el-checkbox v-model="generateImage">同时生成概念图</el-checkbox>
          </div>
        </div>

        <!-- 锁定/定稿提示 -->
        <div v-else class="chat-locked-notice">
          <el-icon><Lock /></el-icon>
          <span>{{ canvas.locked ? '画布已锁定(定金支付后)' : '画布已定稿,重新编辑可继续修改' }}</span>
        </div>
      </div>

      <!-- 右侧:展示区 -->
      <div class="preview-panel">
        <div class="preview-header">
          <el-icon><Picture /></el-icon>
          <span>设计展示</span>
        </div>

        <el-tabs v-model="previewTab" class="preview-tabs">
          <!-- 概念图 -->
          <el-tab-pane label="概念图" name="concepts">
            <div v-if="canvas.conceptImageUrls && canvas.conceptImageUrls.length" class="image-grid">
              <div v-for="(url, i) in canvas.conceptImageUrls" :key="i" class="image-item">
                <el-image
                  :src="url"
                  :preview-src-list="canvas.conceptImageUrls"
                  :initial-index="i"
                  fit="contain"
                  class="concept-img"
                />
              </div>
            </div>
            <el-empty v-else description="还没有概念图,与 AI 对话时勾选「同时生成概念图」" />
          </el-tab-pane>

          <!-- 3D 模型 -->
          <el-tab-pane label="3D 模型" name="3d">
            <div v-if="canvas.model3dUrl" class="model-viewer">
              <div class="model-placeholder">
                <el-icon :size="48"><Box /></el-icon>
                <p>3D 参考模型已生成</p>
                <el-button type="primary" tag="a" :href="canvas.model3dUrl" target="_blank">
                  下载 .glb 文件
                </el-button>
                <p class="model-hint">供厂家原型师精修参考,非直接可打印文件</p>
              </div>
            </div>
            <el-empty v-else description="定稿后自动生成 3D 参考模型" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getCanvasDetail, finalizeCanvas, reopenCanvas, chatWithAI, uploadReferenceImage } from '@/api/canvas'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Check, ChatDotRound, User, MagicStick, Picture,
  Promotion, Close, Lock, Box
} from '@element-plus/icons-vue'

const route = useRoute()
const loading = ref(true)
const canvas = ref({})
const chatMessages = ref([])
const inputText = ref('')
const uploadedImages = ref([])
const generateImage = ref(false)
const sending = ref(false)
const aiTyping = ref(false)
const typingText = ref('')
const finalizing = ref(false)
const previewTab = ref('concepts')
const messagesRef = ref()

// 加载画布详情
async function loadCanvas() {
  loading.value = true
  try {
    const res = await getCanvasDetail(route.params.id)
    canvas.value = res.data
    // 恢复对话记录
    if (res.data.aiConversation) {
      chatMessages.value = res.data.aiConversation.map(m => ({
        role: m.role,
        content: m.content || '',
        images: m.images || []
      }))
    }
  } finally {
    loading.value = false
  }
}

// 上传参考图
async function handleUpload(file) {
  try {
    const res = await uploadReferenceImage(file)
    uploadedImages.value.push(res.data.url)
    ElMessage.success('图片上传成功')
  } catch (e) {
    // 错误已由拦截器处理
  }
  return false // 阻止 el-upload 默认行为
}

// 发送消息
async function handleSend() {
  if (!inputText.value.trim() && uploadedImages.value.length === 0) {
    ElMessage.warning('请输入消息或上传参考图')
    return
  }

  const message = inputText.value.trim()
  const imageUrls = [...uploadedImages.value]

  // 显示用户消息
  chatMessages.value.push({
    role: 'user',
    content: message || '(上传了参考图)',
    images: imageUrls
  })

  // 清空输入
  inputText.value = ''
  uploadedImages.value = []

  // 开始 AI 输入
  sending.value = true
  aiTyping.value = true
  typingText.value = ''

  await nextTick()
  scrollToBottom()

  // 调用 AI 对话
  let fullResponse = ''
  await chatWithAI(route.params.id, {
    message,
    imageUrls: imageUrls.length ? imageUrls : undefined,
    generateImage: generateImage.value
  }, {
    onMessage: (token) => {
      fullResponse += token
      typingText.value = fullResponse
      scrollToBottom()
    },
    onImageGenerating: () => {
      ElMessage.info('正在生成概念图...')
    },
    onImageGenerated: (url) => {
      if (!canvas.value.conceptImageUrls) {
        canvas.value.conceptImageUrls = []
      }
      canvas.value.conceptImageUrls.push(url)
      ElMessage.success('概念图生成成功')
    },
    onImageError: (err) => {
      ElMessage.error('概念图生成失败')
    },
    onDone: () => {
      // 将 AI 回复加入消息列表
      if (fullResponse) {
        chatMessages.value.push({
          role: 'assistant',
          content: fullResponse
        })
      }
      aiTyping.value = false
      typingText.value = ''
      sending.value = false
      scrollToBottom()
    },
    onError: (err) => {
      aiTyping.value = false
      sending.value = false
      ElMessage.error('AI 对话失败,请稍后重试')
    }
  })
}

// 定稿
async function handleFinalize() {
  try {
    await ElMessageBox.confirm('定稿后将触发 3D 模型生成,确定要定稿吗?', '画布定稿', { type: 'warning' })
  } catch {
    return
  }

  finalizing.value = true
  try {
    const res = await finalizeCanvas(route.params.id)
    canvas.value = { ...canvas.value, ...res.data, status: 'FINALIZED' }
    ElMessage.success('画布已定稿,3D 模型生成中...')
    // 重新加载获取最新 3D 模型 URL
    setTimeout(() => loadCanvas(), 3000)
  } finally {
    finalizing.value = false
  }
}

// 重新编辑
async function handleReopen() {
  try {
    await ElMessageBox.confirm('重新编辑将回到设计状态,确定吗?', '重新编辑', { type: 'warning' })
  } catch {
    return
  }
  const res = await reopenCanvas(route.params.id)
  canvas.value = { ...canvas.value, ...res.data, status: 'DESIGNING' }
  ElMessage.success('画布已重新打开')
}

function useSuggestion(text) {
  inputText.value = text
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

onMounted(loadCanvas)
</script>

<style scoped>
.canvas-design { display: flex; flex-direction: column; height: calc(100vh - 64px); padding: 16px; max-width: 1400px; margin: 0 auto; }

.toolbar { display: flex; align-items: center; justify-content: space-between; padding: 8px 16px; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; margin-bottom: 12px; }
.toolbar-left { display: flex; align-items: center; gap: 8px; }
.canvas-status { display: flex; gap: 4px; }

.main-content { flex: 1; display: flex; gap: 12px; overflow: hidden; }

/* 左侧对话 */
.chat-panel { width: 45%; display: flex; flex-direction: column; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; overflow: hidden; }
.chat-header { padding: 12px 16px; border-bottom: 1px solid rgba(255,255,255,0.06); display: flex; align-items: center; gap: 8px; font-weight: 600; color: #fff; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px; }

/* 空状态欢迎 */
.chat-welcome { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; text-align: center; gap: 12px; padding: 24px; }
.welcome-icon { width: 72px; height: 72px; border-radius: 50%; background: rgba(102,126,234,0.1); display: flex; align-items: center; justify-content: center; margin-bottom: 8px; }
.chat-welcome h3 { font-size: 18px; font-weight: 600; color: #fff; }
.chat-welcome p { font-size: 14px; color: #888; max-width: 300px; line-height: 1.6; }
.welcome-suggestions { display: flex; flex-direction: column; gap: 8px; margin-top: 16px; width: 100%; max-width: 320px; }
.suggestion-chip { padding: 10px 16px; border: 1px solid rgba(255,255,255,0.08); border-radius: 10px; cursor: pointer; transition: all 0.2s; font-size: 13px; color: #aaa; }
.suggestion-chip:hover { border-color: rgba(102,126,234,0.3); background: rgba(102,126,234,0.05); color: #fff; }

.message { display: flex; gap: 8px; margin-bottom: 16px; }
.message-avatar { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.message.user .message-avatar { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
.message.assistant .message-avatar { background: linear-gradient(135deg, #43e97b, #38f9d7); color: #fff; }
.message-content { flex: 1; }
.message-text { padding: 10px 14px; border-radius: 8px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; color: #ddd; }
.message.user .message-text { background: rgba(102,126,234,0.1); }
.message.assistant .message-text { background: rgba(67,233,123,0.08); }
.message-images { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.msg-img { width: 120px; height: 120px; border-radius: 6px; }
.typing::after { content: '▋'; animation: blink 1s infinite; }
@keyframes blink { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }

.chat-input { border-top: 1px solid rgba(255,255,255,0.06); padding: 12px; }
.uploaded-images { display: flex; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
.uploaded-img-wrapper { position: relative; }
.uploaded-img { width: 80px; height: 80px; border-radius: 6px; }
.remove-img-btn { position: absolute; top: -8px; right: -8px; }
.input-row { display: flex; gap: 8px; align-items: flex-end; }
.input-options { margin-top: 8px; }
.chat-locked-notice { padding: 20px; text-align: center; color: #666; border-top: 1px solid rgba(255,255,255,0.06); display: flex; align-items: center; justify-content: center; gap: 8px; }

/* 右侧展示 */
.preview-panel { flex: 1; display: flex; flex-direction: column; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; overflow: hidden; }
.preview-header { padding: 12px 16px; border-bottom: 1px solid rgba(255,255,255,0.06); display: flex; align-items: center; gap: 8px; font-weight: 600; color: #fff; }
.preview-tabs { flex: 1; overflow: hidden; display: flex; flex-direction: column; }
.preview-tabs :deep(.el-tabs__content) { flex: 1; overflow-y: auto; padding: 16px; }

.image-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.image-item { border: 1px solid rgba(255,255,255,0.06); border-radius: 8px; overflow: hidden; }
.concept-img { width: 100%; height: 240px; cursor: pointer; }

.model-viewer { display: flex; align-items: center; justify-content: center; height: 100%; }
.model-placeholder { text-align: center; padding: 40px; color: #ccc; }
.model-hint { color: #666; font-size: 12px; margin-top: 8px; }
</style>
