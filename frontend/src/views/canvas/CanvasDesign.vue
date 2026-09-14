<template>
  <div class="canvas-design" v-loading="loading">
    <!-- 顶部工具栏 -->
    <div class="toolbar glass">
      <div class="toolbar-left">
        <el-button text @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回系列
        </el-button>
        <el-divider direction="vertical" />
        <span class="canvas-name">{{ canvas.name || `画布 #${canvas.id || ''}` }}</span>
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
      <div class="chat-panel glass">
        <div class="chat-header">
          <span class="chat-header-dot"></span>
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 设计助手</span>
        </div>

        <!-- 对话记录 -->
        <div class="chat-messages" ref="messagesRef">
          <!-- 空状态欢迎消息 -->
          <div v-if="chatMessages.length === 0 && !aiTyping" class="chat-welcome">
            <div class="welcome-icon">
              <el-icon :size="40" color="#6d7cff"><MagicStick /></el-icon>
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
      <div class="preview-panel glass">
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
            <!-- 生成中:显示进度并自动轮询,不再让请求卡在定稿接口上 -->
            <div v-if="isModel3dRunning" class="model3d-state">
              <el-icon class="spin" :size="36"><Loading /></el-icon>
              <p class="state-title">3D 模型生成中…</p>
              <p class="state-hint">已提交到生成服务,完成后会自动显示。可以切换到其他标签继续设计。</p>
            </div>

            <div v-else-if="canvas.model3dStatus === 'FAILED'" class="model3d-state">
              <el-icon :size="36" class="state-error"><WarningFilled /></el-icon>
              <p class="state-title">3D 模型生成失败</p>
              <p class="state-hint">可以重新生成,不会影响已定稿的设计。</p>
              <el-button type="primary" round :loading="retrying" @click="handleRetryModel3d">
                重新生成
              </el-button>
            </div>

            <Model3DViewer v-else :src="canvas.model3dUrl" :height="440" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getCanvasDetail, finalizeCanvas, reopenCanvas, chatWithAI, uploadReferenceImage, retryModel3d } from '@/api/canvas'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Check, ChatDotRound, User, MagicStick, Picture,
  Promotion, Close, Lock, Box, Loading, WarningFilled
} from '@element-plus/icons-vue'
import Model3DViewer from '@/components/Model3DViewer.vue'
import { onBeforeUnmount } from 'vue'

const route = useRoute()
const loading = ref(true)
const canvas = ref({})
const retrying = ref(false)

// 3D 生成是否已提交但尚未完成
const isModel3dRunning = computed(
  () => canvas.value.model3dStatus === 'PENDING' || canvas.value.model3dStatus === 'PROCESSING'
)

// 生成中时轮询后端状态,生成完自动切换为预览
let model3dTimer = null
function syncModel3dPolling() {
  if (isModel3dRunning.value && !model3dTimer) {
    model3dTimer = setInterval(async () => {
      try {
        const res = await getCanvasDetail(route.params.id)
        canvas.value = res.data
      } catch (e) { /* 忽略单次轮询失败 */ }
      if (!isModel3dRunning.value) stopModel3dPolling()
    }, 5000)
  } else if (!isModel3dRunning.value) {
    stopModel3dPolling()
  }
}
function stopModel3dPolling() {
  if (model3dTimer) {
    clearInterval(model3dTimer)
    model3dTimer = null
  }
}
watch(() => canvas.value.model3dStatus, syncModel3dPolling)
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
let chatAbort = null
onBeforeUnmount(() => {
  chatAbort?.abort()
  stopModel3dPolling()
})

async function handleSend() {
  if (sending.value) return
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
  chatAbort?.abort()
  chatAbort = new AbortController()
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
      ElMessage.error(err?.message || 'AI 对话失败,请稍后重试')
    }
  }, { signal: chatAbort.signal })
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
    // 生成已改为异步,立即刷新一次拿到真实状态,之后由轮询跟进
    await loadCanvas()
  } finally {
    finalizing.value = false
  }
}

// 重新生成 3D 模型(失败后重试)
async function handleRetryModel3d() {
  retrying.value = true
  try {
    const res = await retryModel3d(route.params.id)
    canvas.value = { ...canvas.value, ...res.data }
    ElMessage.success('已重新提交生成')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '重试失败')
  } finally {
    retrying.value = false
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
.canvas-design {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 66px);
  padding: 16px;
  max-width: 1400px;
  margin: 0 auto;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-radius: 14px;
  margin-bottom: 12px;
  border: 1px solid var(--line);
}
.toolbar-left { display: flex; align-items: center; gap: 8px; }
.canvas-name { color: var(--text-1); font-weight: 600; margin-right: 8px; }
.canvas-status { display: flex; gap: 4px; }

.main-content { flex: 1; display: flex; gap: 12px; overflow: hidden; }

/* 左侧对话 */
.chat-panel {
  width: 45%;
  display: flex;
  flex-direction: column;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--line);
}
.chat-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #fff;
}

.chat-header-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: var(--brand-green);
  box-shadow: 0 0 8px rgba(52, 211, 153, 0.8);
  animation: pulse 2s ease-in-out infinite;
}

.chat-messages { flex: 1; overflow-y: auto; padding: 16px; }

/* 空状态欢迎 */
.chat-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  gap: 12px;
  padding: 24px;
}
.welcome-icon {
  width: 76px; height: 76px;
  border-radius: 50%;
  background: rgba(109, 124, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  box-shadow: 0 0 32px rgba(109, 124, 255, 0.15);
  animation: float 4s ease-in-out infinite;
}
.chat-welcome h3 { font-size: 18px; font-weight: 600; color: #fff; }
.chat-welcome p { font-size: 14px; color: var(--text-3); max-width: 300px; line-height: 1.6; }
.welcome-suggestions { display: flex; flex-direction: column; gap: 8px; margin-top: 16px; width: 100%; max-width: 320px; }
.suggestion-chip {
  padding: 10px 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s var(--ease-out);
  font-size: 13px;
  color: var(--text-2);
  background: rgba(255, 255, 255, 0.02);
}
.suggestion-chip:hover {
  border-color: rgba(109, 124, 255, 0.4);
  background: rgba(109, 124, 255, 0.08);
  color: #fff;
  transform: translateX(4px);
}

.message {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  animation: fadeInUp 0.4s var(--ease-out);
}
.message-avatar {
  width: 34px; height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.message.user .message-avatar {
  background: linear-gradient(135deg, #6d7cff, #a855f7);
  color: #fff;
  box-shadow: 0 0 12px rgba(109, 124, 255, 0.3);
}
.message.assistant .message-avatar {
  background: linear-gradient(135deg, #34d399, #22d3ee);
  color: #fff;
  box-shadow: 0 0 12px rgba(52, 211, 153, 0.3);
}
.message-content { flex: 1; }
.message-text {
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text-1);
}
.message.user .message-text { background: rgba(109, 124, 255, 0.1); border: 1px solid rgba(109, 124, 255, 0.15); }
.message.assistant .message-text { background: rgba(52, 211, 153, 0.08); border: 1px solid rgba(52, 211, 153, 0.12); }
.message-images { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.msg-img { width: 120px; height: 120px; border-radius: 8px; }
.typing::after { content: '▋'; animation: blink 1s infinite; }
@keyframes blink { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }

.chat-input { border-top: 1px solid var(--line); padding: 12px; }
.uploaded-images { display: flex; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
.uploaded-img-wrapper { position: relative; }
.uploaded-img { width: 80px; height: 80px; border-radius: 8px; }
.remove-img-btn { position: absolute; top: -8px; right: -8px; }
.input-row { display: flex; gap: 8px; align-items: flex-end; }
.input-options { margin-top: 8px; }
.chat-locked-notice {
  padding: 20px;
  text-align: center;
  color: var(--text-4);
  border-top: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* 右侧展示 */
.preview-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--line);
}
.preview-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #fff;
}
.preview-tabs { flex: 1; overflow: hidden; display: flex; flex-direction: column; }
.preview-tabs :deep(.el-tabs__content) { flex: 1; overflow-y: auto; padding: 16px; }

.image-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.image-item {
  border: 1px solid var(--line);
  border-radius: 10px;
  overflow: hidden;
  transition: transform 0.3s var(--ease-out);
}
.image-item:hover { transform: scale(1.02); }
.concept-img { width: 100%; height: 240px; cursor: pointer; }

.model-viewer { display: flex; align-items: center; justify-content: center; height: 100%; }
.model-placeholder { text-align: center; padding: 40px; color: var(--text-2); }
.model-orb {
  width: 96px; height: 96px;
  margin: 0 auto 20px;
  border-radius: 50%;
  background: rgba(109, 124, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--brand-1);
  box-shadow: 0 0 40px rgba(109, 124, 255, 0.15);
  animation: float 4s ease-in-out infinite;
}
.model-hint { color: var(--text-4); font-size: 12px; margin-top: 8px; }

.model3d-state {
  height: 440px; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 10px;
  text-align: center; padding: 24px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
}
.model3d-state .state-title { margin: 4px 0 0; font-size: 15px; color: var(--text-1); }
.model3d-state .state-hint { margin: 0; font-size: 12px; color: var(--text-3); max-width: 420px; }
.model3d-state .state-error { color: var(--el-color-warning); }
.model3d-state .spin { animation: spin 1s linear infinite; color: var(--brand-cyan); }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
