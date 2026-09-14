<template>
  <div class="model3d-viewer" :style="{ height: height + 'px' }">
    <div v-if="!isPreviewable" class="viewer-fallback">
      <el-icon :size="40"><Box /></el-icon>
      <p class="fallback-title">{{ fallbackTitle }}</p>
      <p class="fallback-hint">{{ fallbackHint }}</p>
      <el-button v-if="src" type="primary" tag="a" :href="src" target="_blank" round>
        下载文件
      </el-button>
    </div>

    <template v-else>
      <div ref="stageEl" class="viewer-stage"></div>

      <div class="viewer-toolbar">
        <el-button size="small" round @click="toggleRotate">
          {{ autoRotate ? '停止旋转' : '自动旋转' }}
        </el-button>
        <el-button size="small" round @click="resetView">重置视角</el-button>
        <el-button size="small" round tag="a" :href="src" target="_blank">下载 .glb</el-button>
      </div>

      <div v-if="state === 'loading'" class="viewer-mask">
        <el-icon class="spin" :size="28"><Loading /></el-icon>
        <span>模型加载中…</span>
      </div>
      <div v-else-if="state === 'error'" class="viewer-mask error">
        <el-icon :size="28"><WarningFilled /></el-icon>
        <span>{{ errorMessage }}</span>
      </div>

      <p class="viewer-hint">拖动旋转 · 滚轮缩放 · 模型为精修参考,非直接可打印文件</p>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { Box, Loading, WarningFilled } from '@element-plus/icons-vue'

const props = defineProps({
  src: { type: String, default: '' },
  height: { type: Number, default: 420 }
})

const stageEl = ref(null)
const state = ref('loading') // loading | ready | error
const errorMessage = ref('')
const autoRotate = ref(true)

/**
 * 只有 glb/gltf 才能交给 three 渲染。
 * 未配置 Meshy 密钥时后端返回的是占位图片,此时必须降级成下载入口,
 * 否则 GLTFLoader 会抛解析错误。
 */
const isPreviewable = computed(() => /\.(glb|gltf)(\?|#|$)/i.test(props.src || ''))
const fallbackTitle = computed(() => (props.src ? '当前文件不是 3D 模型' : '暂无 3D 模型'))
const fallbackHint = computed(() =>
  props.src
    ? '未配置 MESHY_API_KEY 时,定稿返回的是占位图片而非 .glb 模型'
    : '定稿后自动生成 3D 参考模型'
)

let renderer, scene, camera, controls, model, frameId
let threeLibs = null

async function loadThree() {
  if (!threeLibs) {
    const [THREE, { OrbitControls }, { GLTFLoader }] = await Promise.all([
      import('three'),
      import('three/examples/jsm/controls/OrbitControls.js'),
      import('three/examples/jsm/loaders/GLTFLoader.js')
    ])
    threeLibs = { THREE, OrbitControls, GLTFLoader }
  }
  return threeLibs
}

function dispose() {
  window.removeEventListener('resize', onResize)
  cancelAnimationFrame(frameId)
  controls?.dispose?.()
  if (model) {
    model.traverse?.((o) => {
      o.geometry?.dispose?.()
      if (o.material) {
        const mats = Array.isArray(o.material) ? o.material : [o.material]
        mats.forEach((m) => m.dispose?.())
      }
    })
  }
  if (renderer) {
    renderer.dispose()
    if (renderer.domElement?.parentNode === stageEl.value) {
      stageEl.value.removeChild(renderer.domElement)
    }
  }
  renderer = scene = camera = controls = model = null
}

async function init() {
  if (!stageEl.value || !isPreviewable.value) return
  state.value = 'loading'
  errorMessage.value = ''

  try {
    const { THREE, OrbitControls, GLTFLoader } = await loadThree()
    const el = stageEl.value
    const w = el.clientWidth || 600
    const h = props.height - 56

    scene = new THREE.Scene()
    scene.background = new THREE.Color(0x0b0b14)

    camera = new THREE.PerspectiveCamera(45, w / h, 0.01, 1000)
    camera.position.set(0, 1.2, 3.2)

    renderer = new THREE.WebGLRenderer({ antialias: true })
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    renderer.setSize(w, h)
    el.appendChild(renderer.domElement)

    scene.add(new THREE.AmbientLight(0xffffff, 1.1))
    const key = new THREE.DirectionalLight(0xffffff, 2.2)
    key.position.set(3, 5, 4)
    scene.add(key)
    const rim = new THREE.DirectionalLight(0x6d7cff, 1.0)
    rim.position.set(-4, 2, -3)
    scene.add(rim)

    controls = new OrbitControls(camera, renderer.domElement)
    controls.enableDamping = true
    controls.autoRotate = autoRotate.value
    controls.autoRotateSpeed = 1.6

    const loader = new GLTFLoader()
    loader.load(
      props.src,
      (gltf) => {
        model = gltf.scene
        // 自动居中并缩放到合适大小,避免模型过大或过小
        const box = new THREE.Box3().setFromObject(model)
        const size = box.getSize(new THREE.Vector3())
        const center = box.getCenter(new THREE.Vector3())
        const maxDim = Math.max(size.x, size.y, size.z) || 1
        model.position.sub(center)
        const scale = 1.6 / maxDim
        model.scale.setScalar(scale)
        scene.add(model)
        state.value = 'ready'
      },
      undefined,
      () => {
        state.value = 'error'
        errorMessage.value = '模型加载失败,可能是地址无效或跨域受限'
      }
    )

    const animate = () => {
      frameId = requestAnimationFrame(animate)
      controls.update()
      renderer.render(scene, camera)
    }
    animate()
    window.addEventListener('resize', onResize)
  } catch (e) {
    state.value = 'error'
    errorMessage.value = '3D 渲染初始化失败'
  }
}

function onResize() {
  if (!renderer || !camera || !stageEl.value) return
  const w = stageEl.value.clientWidth || 600
  const h = props.height - 56
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

function toggleRotate() {
  autoRotate.value = !autoRotate.value
  if (controls) controls.autoRotate = autoRotate.value
}

function resetView() {
  if (!camera || !controls) return
  camera.position.set(0, 1.2, 3.2)
  controls.target.set(0, 0, 0)
  controls.update()
}

onMounted(init)
onBeforeUnmount(dispose)
watch(() => props.src, () => {
  dispose()
  init()
})
</script>

<style scoped>
.model3d-viewer {
  position: relative;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--line);
}
.viewer-stage { width: 100%; height: calc(100% - 56px); }
.viewer-toolbar {
  position: absolute; top: 12px; right: 12px;
  display: flex; gap: 8px; z-index: 2;
}
.viewer-hint {
  position: absolute; left: 0; right: 0; bottom: 8px;
  text-align: center; margin: 0;
  font-size: 12px; color: var(--text-3); pointer-events: none;
}
.viewer-mask {
  position: absolute; inset: 0;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 10px; font-size: 13px; color: var(--text-2);
  background: rgba(11, 11, 20, 0.72);
}
.viewer-mask.error { color: var(--el-color-danger); }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.viewer-fallback {
  height: 100%; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 10px;
  color: var(--text-2); text-align: center; padding: 24px;
}
.fallback-title { margin: 4px 0 0; font-size: 15px; color: var(--text-1); }
.fallback-hint { margin: 0; font-size: 12px; color: var(--text-3); max-width: 420px; }
</style>
