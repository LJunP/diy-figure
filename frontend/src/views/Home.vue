<template>
  <div class="home">
    <!-- ===== Hero 全屏区域 ===== -->
    <section class="hero" @mousemove="handleParallax">
      <div class="hero-orb orb-1" :style="orbStyle(0)"></div>
      <div class="hero-orb orb-2" :style="orbStyle(1)"></div>
      <div class="hero-orb orb-3" :style="orbStyle(2)"></div>
      <div class="hero-rings">
        <div class="ring r1"></div>
        <div class="ring r2"></div>
        <div class="ring r3"></div>
      </div>

      <div class="hero-content" :style="contentStyle">
        <div class="hero-badge">
          <span class="badge-dot"></span>
          AI × 手办盲盒 · 全新定制体验
        </div>
        <h1 class="hero-title">
          用 <span class="text-gradient">AI 对话</span> 设计
          <br />
          属于你的原创手办
        </h1>
        <p class="hero-subtitle">
          与 GPT-4o 协作设计原创角色形象,一键生成概念图与 3D 参考模型,
          <br />
          经人工终审后量产实体盲盒。从灵感到实物,全流程一站式定制。
        </p>
        <div class="hero-actions">
          <el-button size="large" round class="cta-btn" @click="startDesign">
            <el-icon class="el-icon--left"><MagicStick /></el-icon>
            开始设计
          </el-button>
          <el-button size="large" round class="ghost-btn" @click="scrollToProcess">
            了解流程
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
        </div>
        <div class="hero-stats">
          <div class="hero-stat">
            <span class="stat-num">3</span>
            <span class="stat-label">规格档位</span>
          </div>
          <div class="hero-stat-sep"></div>
          <div class="hero-stat">
            <span class="stat-num">14</span>
            <span class="stat-label">订单状态</span>
          </div>
          <div class="hero-stat-sep"></div>
          <div class="hero-stat">
            <span class="stat-num">∞</span>
            <span class="stat-label">创意可能</span>
          </div>
        </div>
      </div>
      <div class="scroll-hint">
        <span class="mouse"><span class="wheel"></span></span>
      </div>
    </section>

    <!-- ===== 功能亮点 ===== -->
    <section class="features-section">
      <div class="section-header" v-reveal>
        <span class="section-label">FEATURES</span>
        <h2 class="section-title">核心功能</h2>
        <p class="section-desc">AI 驱动的原创手办盲盒定制全流程</p>
      </div>
      <div class="features-grid">
        <div
          v-for="(feature, i) in features"
          :key="feature.title"
          class="feature-card glass-card"
          v-reveal="{ index: i }"
        >
          <div class="feature-glow" :style="{ background: feature.bg }"></div>
          <div class="feature-icon" :style="{ background: feature.bg }">
            <el-icon :size="28"><component :is="feature.icon" /></el-icon>
          </div>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.desc }}</p>
          <ul class="feature-list">
            <li v-for="item in feature.items" :key="item">{{ item }}</li>
          </ul>
        </div>
      </div>
    </section>

    <!-- ===== 定制流程 ===== -->
    <section id="process" class="process-section">
      <div class="section-header" v-reveal>
        <span class="section-label">WORKFLOW</span>
        <h2 class="section-title">定制流程</h2>
        <p class="section-desc">从 AI 对话到收到实体盲盒,五步完成定制</p>
      </div>
      <div class="process-timeline">
        <div class="timeline-line"></div>
        <div
          v-for="(step, index) in steps"
          :key="index"
          class="timeline-item"
          v-reveal="{ delay: index * 0.08 }"
        >
          <div class="timeline-dot">
            <span class="dot-number">{{ index + 1 }}</span>
            <span class="dot-ring"></span>
          </div>
          <div class="timeline-card glass-card">
            <div class="timeline-icon">
              <el-icon :size="24"><component :is="step.icon" /></el-icon>
            </div>
            <h3>{{ step.title }}</h3>
            <p>{{ step.desc }}</p>
            <div class="timeline-tags">
              <el-tag v-for="tag in step.tags" :key="tag" size="small" effect="dark" class="timeline-tag">
                {{ tag }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 规格档位 ===== -->
    <section class="tier-section">
      <div class="section-header" v-reveal>
        <span class="section-label">PRICING</span>
        <h2 class="section-title">规格档位</h2>
        <p class="section-desc">三种档位,满足不同收藏需求</p>
      </div>
      <div class="tier-grid">
        <div
          v-for="(tier, i) in tiers"
          :key="tier.name"
          class="tier-card glow-border"
          :class="{ featured: tier.featured }"
          v-reveal="{ index: i }"
        >
          <div v-if="tier.featured" class="tier-badge">推荐</div>
          <h3 class="tier-name">{{ tier.name }}</h3>
          <div class="tier-price">
            <span class="tier-design">{{ tier.design }}</span>
            <span class="tier-unit">个角色设计</span>
          </div>
          <div class="tier-selected">
            <span class="selected-num">{{ tier.selected }}</span>
            <span class="selected-label">个中签量产</span>
          </div>
          <el-divider />
          <ul class="tier-features">
            <li v-for="feature in tier.features" :key="feature">
              <el-icon color="#6d7cff"><Check /></el-icon>
              <span>{{ feature }}</span>
            </li>
          </ul>
          <el-button :type="tier.featured ? 'primary' : 'default'" round class="tier-btn" @click="selectTier(tier.value)">
            选择此档位
          </el-button>
        </div>
      </div>
    </section>

    <!-- ===== 订单状态机展示 ===== -->
    <section class="status-section">
      <div class="section-header" v-reveal>
        <span class="section-label">STATE MACHINE</span>
        <h2 class="section-title">订单全流程状态管理</h2>
        <p class="section-desc">14 个订单状态 + 严格的状态机流转,保障交易安全</p>
      </div>
      <div class="status-flow">
        <div v-for="(stage, index) in orderStages" :key="index" class="status-node" v-reveal="{ index }">
          <div class="status-card glass-card" :style="{ '--stage-color': stage.color }">
            <el-icon :size="20" :color="stage.color"><component :is="stage.icon" /></el-icon>
            <span class="status-name">{{ stage.name }}</span>
          </div>
          <el-icon v-if="index < orderStages.length - 1" class="status-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </section>

    <!-- ===== CTA 区域 ===== -->
    <section class="cta-section">
      <div class="cta-glow"></div>
      <div class="cta-content" v-reveal>
        <h2 class="cta-title">准备好开始了吗?</h2>
        <p class="cta-desc">立即注册,与 AI 一起设计你的第一个原创手办角色</p>
        <el-button size="large" round class="cta-final-btn" @click="startDesign">
          <el-icon class="el-icon--left"><MagicStick /></el-icon>
          免费开始设计
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  MagicStick, ArrowDown, ArrowRight, Check,
  ChatDotRound, Document, Trophy, Box, Setting,
  Picture, View, Monitor, Wallet
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const parallax = reactive({ x: 0, y: 0 })
const contentStyle = ref({})

function handleParallax(e) {
  const rect = e.currentTarget.getBoundingClientRect()
  const x = (e.clientX - rect.left) / rect.width - 0.5
  const y = (e.clientY - rect.top) / rect.height - 0.5
  parallax.x = x
  parallax.y = y
  contentStyle.value = {
    transform: `translate(${x * -12}px, ${y * -12}px)`
  }
}

function orbStyle(i) {
  const factor = [18, -22, 14][i] || 10
  return {
    transform: `translate(${parallax.x * factor}px, ${parallax.y * factor}px)`
  }
}

const features = [
  {
    icon: 'ChatDotRound',
    title: 'AI 对话式设计',
    desc: '与 GPT-4o 实时对话,文字+图片输入,逐步细化角色设计',
    bg: 'linear-gradient(135deg, #6d7cff 0%, #a855f7 100%)',
    items: ['SSE 流式回复', '支持参考图上传', '多轮对话记忆', '合规红线过滤']
  },
  {
    icon: 'Picture',
    title: 'AI 概念图生成',
    desc: 'DALL-E 3 根据对话描述生成 2D 三视图概念图',
    bg: 'linear-gradient(135deg, #f093fb 0%, #ec4899 100%)',
    items: ['正/侧/背三视图', '高清 1024px', '原创风格保证', 'OSS 云端存储']
  },
  {
    icon: 'View',
    title: '3D 参考模型',
    desc: 'Meshy AI 图生 3D,定稿后自动生成 .glb 参考模型',
    bg: 'linear-gradient(135deg, #22d3ee 0%, #34d399 100%)',
    items: ['图生 3D 自动化', 'PBR 纹理', '.glb 格式下载', '供厂家原型师参考']
  },
  {
    icon: 'Document',
    title: '人工终审',
    desc: '运营后台硬性审核,确保原创合规后才进入量产',
    bg: 'linear-gradient(135deg, #34d399 0%, #38f9d7 100%)',
    items: ['硬性阻塞点', '运营手动审核', '审核日志记录', '拒绝可修改重提']
  },
  {
    icon: 'Trophy',
    title: '抽奖机制',
    desc: '接受报价后手动触发抽奖,按档位比例随机抽中量产角色',
    bg: 'linear-gradient(135deg, #fa709a 0%, #fbbf24 100%)',
    items: ['用户手动触发', '随机抽取算法', '中签/未中签分流', '未中签可补购']
  },
  {
    icon: 'Box',
    title: '生产发货',
    desc: '定金→生产→质检→尾款→发货,全程状态追踪',
    bg: 'linear-gradient(135deg, #a855f7 0%, #6d7cff 100%)',
    items: ['双阶段付款', '质检流程', '物流追踪', '签收完成']
  }
]

const steps = [
  {
    icon: 'ChatDotRound',
    title: 'AI 对话设计',
    desc: '创建系列后,与 AI 对话设计原创角色。支持文字描述和参考图输入,AI 会给出详细的角色外观、色彩方案和背景故事。',
    tags: ['GPT-4o', 'SSE 流式', '多模态']
  },
  {
    icon: 'Picture',
    title: '生成概念图 & 定稿',
    desc: '对设计满意后,一键生成 2D 概念图三视图。定稿后自动触发 Meshy AI 生成 3D 参考模型,供厂家原型师精修参考。',
    tags: ['DALL-E 3', 'Meshy 3D', '定稿锁定']
  },
  {
    icon: 'Document',
    title: '提交报价 & 终审',
    desc: '凑够档位要求的已定稿画布后提交报价申请。运营后台人工终审,确保原创合规后给出报价和预计交货日期。',
    tags: ['人工终审', '报价', '交期']
  },
  {
    icon: 'Trophy',
    title: '抽奖 & 付款',
    desc: '接受报价后手动触发抽奖,系统按档位比例随机抽中量产角色。填写收货地址后支付定金(50%),进入生产环节。',
    tags: ['随机抽奖', '定金 50%', '收货地址']
  },
  {
    icon: 'Box',
    title: '生产 · 质检 · 发货',
    desc: '运营标记开工→生产→质检通过→付尾款→发货。收到盲盒后确认签收,订单完成。未中签角色可在 60 天内补购。',
    tags: ['生产追踪', '质检', '尾款 50%', '补购']
  }
]

const tiers = [
  {
    name: '轻量系列', value: 'LIGHT', design: 6, selected: 4, featured: false,
    features: ['6 个原创角色设计', '4 个中签量产', '2 个可补购', 'Q 版/写实可选', '小型尺寸(约 6cm)']
  },
  {
    name: '经典系列', value: 'CLASSIC', design: 9, selected: 6, featured: true,
    features: ['9 个原创角色设计', '6 个中签量产', '3 个可补购', 'Q 版/写实可选', '中型尺寸(约 10cm)', '优先排产']
  },
  {
    name: '收藏系列', value: 'COLLECTION', design: 12, selected: 8, featured: false,
    features: ['12 个原创角色设计', '8 个中签量产', '4 个可补购', 'Q 版/写实可选', '大型尺寸(约 15cm)', '限量编号']
  }
]

const orderStages = [
  { name: '终审中', icon: 'Document', color: '#6d7cff' },
  { name: '已报价', icon: 'Wallet', color: '#ec4899' },
  { name: '已抽奖', icon: 'Trophy', color: '#fa709a' },
  { name: '待付定金', icon: 'Wallet', color: '#fbbf24' },
  { name: '生产中', icon: 'Setting', color: '#34d399' },
  { name: '待质检', icon: 'Monitor', color: '#22d3ee' },
  { name: '待付尾款', icon: 'Wallet', color: '#f093fb' },
  { name: '待发货', icon: 'Box', color: '#a8edea' },
  { name: '已发货', icon: 'Box', color: '#38f9d7' },
  { name: '已完成', icon: 'Check', color: '#34d399' }
]

function startDesign() {
  if (userStore.isLoggedIn) {
    router.push('/series')
  } else {
    router.push('/login')
  }
}

function selectTier(tier) {
  if (userStore.isLoggedIn) {
    router.push({ path: '/series/create', query: { tier } })
  } else {
    router.push({ path: '/login', query: { redirect: '/series/create?tier=' + tier } })
  }
}

function scrollToProcess() {
  document.getElementById('process')?.scrollIntoView({ behavior: 'smooth' })
}
</script>

<style scoped>
.home {
  position: relative;
}

/* ===== Hero ===== */
.hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding-top: 66px;
}

.hero-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  will-change: transform;
}

.orb-1 {
  width: 50vw; height: 50vw; min-width: 460px; min-height: 460px;
  top: -10%; left: -8%;
  background: radial-gradient(circle, rgba(109, 124, 255, 0.28), transparent 65%);
  animation: blobDrift 28s ease-in-out infinite;
}
.orb-2 {
  width: 44vw; height: 44vw; min-width: 420px; min-height: 420px;
  top: 28%; right: -12%;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.22), transparent 65%);
  animation: blobDriftAlt 34s ease-in-out infinite;
}
.orb-3 {
  width: 30vw; height: 30vw; min-width: 320px; min-height: 320px;
  bottom: -8%; left: 30%;
  background: radial-gradient(circle, rgba(236, 72, 153, 0.16), transparent 65%);
  animation: blobDrift 40s ease-in-out infinite reverse;
}

.hero-rings {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}
.ring {
  position: absolute;
  top: 50%; left: 50%;
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}
.r1 { width: 540px; height: 540px; }
.r2 { width: 820px; height: 820px; border-color: rgba(109, 124, 255, 0.06); }
.r3 { width: 1120px; height: 1120px; border-color: rgba(255, 255, 255, 0.03); }

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 0 24px;
  max-width: 920px;
  animation: fadeInUp 1s var(--ease-out);
  transition: transform 0.2s ease-out;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  border-radius: 24px;
  background: rgba(109, 124, 255, 0.12);
  border: 1px solid rgba(109, 124, 255, 0.28);
  color: var(--brand-1);
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 24px;
  backdrop-filter: blur(8px);
}

.badge-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--brand-1);
  box-shadow: 0 0 8px rgba(109, 124, 255, 0.9);
  animation: pulse 2s ease-in-out infinite;
}

.hero-title {
  font-size: 60px;
  font-weight: 800;
  line-height: 1.15;
  color: #fff;
  margin-bottom: 20px;
  letter-spacing: -1px;
}

.hero-subtitle {
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-3);
  margin-bottom: 40px;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-bottom: 60px;
}

.cta-btn {
  background: var(--gradient-brand) !important;
  border: none !important;
  color: #fff !important;
  font-size: 16px;
  padding: 12px 32px;
  height: auto;
  box-shadow: 0 4px 24px rgba(109, 124, 255, 0.4);
  position: relative;
  overflow: hidden;
}

.cta-btn::after {
  content: '';
  position: absolute;
  top: 0; left: -80%;
  width: 50%; height: 100%;
  background: linear-gradient(100deg, transparent 20%, rgba(255,255,255,0.35) 50%, transparent 80%);
  transform: skewX(-20deg);
  transition: left 0.7s var(--ease-out);
}
.cta-btn:hover::after { left: 130%; }

.ghost-btn {
  background: rgba(255, 255, 255, 0.04) !important;
  border: 1px solid rgba(255, 255, 255, 0.18) !important;
  color: var(--text-2) !important;
  font-size: 16px;
  padding: 12px 32px;
  height: auto;
  backdrop-filter: blur(12px);
}

.ghost-btn:hover {
  background: rgba(255, 255, 255, 0.08) !important;
  border-color: rgba(109, 124, 255, 0.4) !important;
  color: #fff !important;
}

.hero-stats {
  display: flex;
  gap: 0;
  justify-content: center;
  align-items: center;
}

.hero-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 0 32px;
}

.hero-stat-sep {
  width: 1px;
  height: 36px;
  background: rgba(255, 255, 255, 0.08);
}

.stat-num {
  font-size: 34px;
  font-weight: 800;
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  font-size: 13px;
  color: var(--text-4);
}

.scroll-hint {
  position: absolute;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1;
}
.mouse {
  display: block;
  width: 22px; height: 36px;
  border: 2px solid rgba(255, 255, 255, 0.25);
  border-radius: 12px;
  position: relative;
}
.wheel {
  position: absolute;
  top: 6px; left: 50%;
  transform: translateX(-50%);
  width: 4px; height: 8px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 2px;
  animation: wheelMove 1.6s ease-in-out infinite;
}
@keyframes wheelMove {
  0% { transform: translateX(-50%) translateY(0); opacity: 1; }
  70% { transform: translateX(-50%) translateY(12px); opacity: 0; }
  100% { transform: translateX(-50%) translateY(0); opacity: 0; }
}

/* ===== 通用 Section ===== */
.section-header {
  text-align: center;
  padding: 88px 24px 52px;
}

.section-label {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 3px;
  color: var(--brand-1);
  margin-bottom: 12px;
}

.section-title {
  font-size: 38px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
  letter-spacing: -0.5px;
}

.section-desc {
  font-size: 15px;
  color: var(--text-3);
}

/* ===== 功能亮点 ===== */
.features-section {
  padding: 0 32px 80px;
  max-width: 1200px;
  margin: 0 auto;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.feature-card {
  border-radius: var(--radius-lg);
  padding: 32px 24px;
  position: relative;
  z-index: 1;
}

.feature-glow {
  position: absolute;
  top: -30%; right: -30%;
  width: 60%; height: 60%;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0;
  transition: opacity 0.5s ease;
  pointer-events: none;
}

.feature-card:hover .feature-glow {
  opacity: 0.35;
}

.feature-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
}

.feature-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 8px;
  position: relative;
  z-index: 1;
}

.feature-card p {
  font-size: 14px;
  color: var(--text-3);
  line-height: 1.6;
  margin-bottom: 16px;
  position: relative;
  z-index: 1;
}

.feature-list {
  list-style: none;
  padding: 0;
  position: relative;
  z-index: 1;
}

.feature-list li {
  font-size: 13px;
  color: var(--text-2);
  padding: 4px 0;
  padding-left: 16px;
  position: relative;
}

.feature-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--brand-1);
}

/* ===== 流程时间线 ===== */
.process-section {
  background: rgba(5, 5, 10, 0.5);
  padding: 0 32px 80px;
  backdrop-filter: blur(4px);
}

.process-timeline {
  max-width: 1000px;
  margin: 0 auto;
  position: relative;
}

.timeline-line {
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 2px;
  background: linear-gradient(180deg, transparent 0%, rgba(109, 124, 255, 0.4) 10%, rgba(168, 85, 247, 0.4) 90%, transparent 100%);
  transform: translateX(-50%);
}

.timeline-item {
  display: flex;
  justify-content: center;
  margin-bottom: 36px;
  position: relative;
}

.timeline-item:nth-child(odd) { justify-content: flex-start; }
.timeline-item:nth-child(even) { justify-content: flex-end; }

.timeline-dot {
  position: absolute;
  left: 50%;
  top: 24px;
  transform: translateX(-50%);
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--bg-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

.dot-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 1px solid rgba(109, 124, 255, 0.35);
  animation: ripple 2.5s ease-out infinite;
}

.dot-number {
  font-size: 14px;
  font-weight: 700;
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.timeline-card {
  width: 42%;
  padding: 24px;
}

.timeline-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(109, 124, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--brand-1);
  margin-bottom: 16px;
}

.timeline-card h3 {
  font-size: 17px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 8px;
}

.timeline-card p {
  font-size: 13px;
  color: var(--text-3);
  line-height: 1.6;
  margin-bottom: 12px;
}

.timeline-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.timeline-tag {
  border-color: rgba(109, 124, 255, 0.3) !important;
  background: rgba(109, 124, 255, 0.1) !important;
  color: var(--brand-1) !important;
}

/* ===== 规格档位 ===== */
.tier-section {
  padding: 0 32px 80px;
  max-width: 1200px;
  margin: 0 auto;
}

.tier-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.tier-card {
  padding: 32px 24px;
  text-align: center;
  position: relative;
  transition: transform 0.45s var(--ease-out);
}

.tier-card:hover {
  transform: translateY(-6px);
}

.tier-card.featured {
  transform: scale(1.04);
}

.tier-card.featured:hover {
  transform: scale(1.04) translateY(-6px);
}

.tier-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  padding: 4px 16px;
  border-radius: 12px;
  background: var(--gradient-brand);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 4px 16px rgba(109, 124, 255, 0.4);
}

.tier-name {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 20px;
}

.tier-price { margin-bottom: 8px; }

.tier-design {
  font-size: 48px;
  font-weight: 800;
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.tier-unit {
  font-size: 14px;
  color: var(--text-4);
  margin-left: 4px;
}

.tier-selected { margin-bottom: 20px; }

.selected-num {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.selected-label {
  font-size: 13px;
  color: var(--text-3);
  margin-left: 4px;
}

.tier-features {
  list-style: none;
  padding: 0;
  text-align: left;
  margin-bottom: 24px;
}

.tier-features li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 14px;
  color: var(--text-2);
}

.tier-btn { width: 100%; }

/* ===== 状态机展示 ===== */
.status-section {
  background: rgba(5, 5, 10, 0.5);
  padding: 0 32px 80px;
  overflow-x: auto;
  backdrop-filter: blur(4px);
}

.status-flow {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  flex-wrap: wrap;
  max-width: 1200px;
  margin: 0 auto;
}

.status-node { display: flex; align-items: center; }

.status-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  min-width: 84px;
  border-color: rgba(255, 255, 255, 0.08);
}

.status-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: var(--stage-color);
  opacity: 0.06;
  pointer-events: none;
}

.status-card:hover {
  border-color: var(--stage-color) !important;
}
.status-card:hover::before { opacity: 0.14; }

.status-name {
  font-size: 12px;
  color: var(--text-2);
  white-space: nowrap;
  position: relative;
  z-index: 1;
}

.status-arrow {
  color: rgba(255, 255, 255, 0.15);
  margin: 0 4px;
  flex-shrink: 0;
}

/* ===== CTA ===== */
.cta-section {
  position: relative;
  padding: 88px 32px;
  text-align: center;
  overflow: hidden;
}

.cta-glow {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 70%; height: 120%;
  background: radial-gradient(ellipse at center, rgba(109, 124, 255, 0.14) 0%, transparent 60%);
  pointer-events: none;
}

.cta-content {
  max-width: 600px;
  margin: 0 auto;
  position: relative;
}

.cta-title {
  font-size: 38px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 16px;
}

.cta-desc {
  font-size: 15px;
  color: var(--text-3);
  margin-bottom: 32px;
}

.cta-final-btn {
  background: var(--gradient-brand) !important;
  border: none !important;
  color: #fff !important;
  font-size: 16px;
  padding: 14px 40px;
  height: auto;
  box-shadow: 0 4px 24px rgba(109, 124, 255, 0.4);
  position: relative;
  overflow: hidden;
}

.cta-final-btn::after {
  content: '';
  position: absolute;
  top: 0; left: -80%;
  width: 50%; height: 100%;
  background: linear-gradient(100deg, transparent 20%, rgba(255,255,255,0.35) 50%, transparent 80%);
  transform: skewX(-20deg);
  transition: left 0.7s var(--ease-out);
}
.cta-final-btn:hover::after { left: 130%; }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .hero-title { font-size: 36px; }
  .features-grid { grid-template-columns: 1fr; }
  .tier-grid { grid-template-columns: 1fr; }
  .tier-card.featured { transform: none; }
  .tier-card.featured:hover { transform: translateY(-6px); }
  .timeline-line { left: 22px; }
  .timeline-item:nth-child(odd),
  .timeline-item:nth-child(even) { justify-content: flex-start; }
  .timeline-dot { left: 22px; }
  .timeline-card { width: calc(100% - 64px); margin-left: 64px; }
  .nav-center { display: none; }
  .hero-stats { gap: 0; }
  .hero-stat { padding: 0 16px; }
  .stat-num { font-size: 24px; }
  .status-flow { flex-direction: column; }
  .status-arrow { transform: rotate(90deg); }
  .hero-rings { display: none; }
}
</style>
