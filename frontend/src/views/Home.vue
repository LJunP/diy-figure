<template>
  <div class="home">
    <!-- ===== Hero 全屏区域 ===== -->
    <section class="hero">
      <div class="hero-bg">
        <div class="hero-gradient"></div>
        <div class="hero-particles"></div>
      </div>
      <div class="hero-content">
        <div class="hero-badge">AI × 手办盲盒 · 全新定制体验</div>
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
          <el-button type="primary" size="large" round class="cta-btn" @click="startDesign">
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
          <div class="hero-stat">
            <span class="stat-num">14</span>
            <span class="stat-label">订单状态</span>
          </div>
          <div class="hero-stat">
            <span class="stat-num">∞</span>
            <span class="stat-label">创意可能</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 功能亮点 ===== -->
    <section class="features-section">
      <div class="section-header">
        <span class="section-label">FEATURES</span>
        <h2 class="section-title">核心功能</h2>
        <p class="section-desc">AI 驱动的原创手办盲盒定制全流程</p>
      </div>
      <div class="features-grid">
        <div v-for="feature in features" :key="feature.title" class="feature-card">
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
      <div class="section-header">
        <span class="section-label">WORKFLOW</span>
        <h2 class="section-title">定制流程</h2>
        <p class="section-desc">从 AI 对话到收到实体盲盒,五步完成定制</p>
      </div>
      <div class="process-timeline">
        <div class="timeline-line"></div>
        <div v-for="(step, index) in steps" :key="index" class="timeline-item">
          <div class="timeline-dot">
            <span class="dot-number">{{ index + 1 }}</span>
          </div>
          <div class="timeline-card">
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
      <div class="section-header">
        <span class="section-label">PRICING</span>
        <h2 class="section-title">规格档位</h2>
        <p class="section-desc">三种档位,满足不同收藏需求</p>
      </div>
      <div class="tier-grid">
        <div v-for="tier in tiers" :key="tier.name" class="tier-card" :class="{ featured: tier.featured }">
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
              <el-icon color="#667eea"><Check /></el-icon>
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
      <div class="section-header">
        <span class="section-label">STATE MACHINE</span>
        <h2 class="section-title">订单全流程状态管理</h2>
        <p class="section-desc">14 个订单状态 + 严格的状态机流转,保障交易安全</p>
      </div>
      <div class="status-flow">
        <div v-for="(stage, index) in orderStages" :key="index" class="status-node">
          <div class="status-card" :style="{ borderColor: stage.color }">
            <el-icon :size="20" :color="stage.color"><component :is="stage.icon" /></el-icon>
            <span class="status-name">{{ stage.name }}</span>
          </div>
          <el-icon v-if="index < orderStages.length - 1" class="status-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </section>

    <!-- ===== CTA 区域 ===== -->
    <section class="cta-section">
      <div class="cta-content">
        <h2 class="cta-title">准备好开始了吗?</h2>
        <p class="cta-desc">立即注册,与 AI 一起设计你的第一个原创手办角色</p>
        <el-button type="primary" size="large" round class="cta-final-btn" @click="startDesign">
          <el-icon class="el-icon--left"><MagicStick /></el-icon>
          免费开始设计
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  MagicStick, ArrowDown, ArrowRight, Check,
  ChatDotRound, Document, Trophy, Box, Setting,
  Picture, View, Monitor, Wallet
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const features = [
  {
    icon: 'ChatDotRound',
    title: 'AI 对话式设计',
    desc: '与 GPT-4o 实时对话,文字+图片输入,逐步细化角色设计',
    bg: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    items: ['SSE 流式回复', '支持参考图上传', '多轮对话记忆', '合规红线过滤']
  },
  {
    icon: 'Picture',
    title: 'AI 概念图生成',
    desc: 'DALL-E 3 根据对话描述生成 2D 三视图概念图',
    bg: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    items: ['正/侧/背三视图', '高清 1024px', '原创风格保证', 'OSS 云端存储']
  },
  {
    icon: 'View',
    title: '3D 参考模型',
    desc: 'Meshy AI 图生 3D,定稿后自动生成 .glb 参考模型',
    bg: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    items: ['图生 3D 自动化', 'PBR 纹理', '.glb 格式下载', '供厂家原型师参考']
  },
  {
    icon: 'Document',
    title: '人工终审',
    desc: '运营后台硬性审核,确保原创合规后才进入量产',
    bg: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    items: ['硬性阻塞点', '运营手动审核', '审核日志记录', '拒绝可修改重提']
  },
  {
    icon: 'Trophy',
    title: '抽奖机制',
    desc: '接受报价后手动触发抽奖,按档位比例随机抽中量产角色',
    bg: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    items: ['用户手动触发', '随机抽取算法', '中签/未中签分流', '未中签可补购']
  },
  {
    icon: 'Box',
    title: '生产发货',
    desc: '定金→生产→质检→尾款→发货,全程状态追踪',
    bg: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
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
  { name: '终审中', icon: 'Document', color: '#667eea' },
  { name: '已报价', icon: 'Wallet', color: '#f5576c' },
  { name: '已抽奖', icon: 'Trophy', color: '#fa709a' },
  { name: '待付定金', icon: 'Wallet', color: '#fee140' },
  { name: '生产中', icon: 'Setting', color: '#43e97b' },
  { name: '待质检', icon: 'Monitor', color: '#4facfe' },
  { name: '待付尾款', icon: 'Wallet', color: '#f093fb' },
  { name: '待发货', icon: 'Box', color: '#a8edea' },
  { name: '已发货', icon: 'Box', color: '#38f9d7' },
  { name: '已完成', icon: 'Check', color: '#43e97b' }
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
  background: #0d0d0d;
}

/* ===== Hero ===== */
.hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.hero-gradient {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 20% 30%, rgba(102, 126, 234, 0.15) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 70%, rgba(118, 75, 162, 0.15) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 100%, rgba(240, 147, 251, 0.08) 0%, transparent 60%);
}

.hero-particles {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(2px 2px at 20% 30%, rgba(255,255,255,0.15), transparent),
    radial-gradient(2px 2px at 60% 70%, rgba(255,255,255,0.1), transparent),
    radial-gradient(1px 1px at 50% 50%, rgba(255,255,255,0.1), transparent),
    radial-gradient(1px 1px at 80% 10%, rgba(255,255,255,0.1), transparent),
    radial-gradient(2px 2px at 90% 60%, rgba(255,255,255,0.08), transparent),
    radial-gradient(1px 1px at 33% 80%, rgba(255,255,255,0.1), transparent),
    radial-gradient(1px 1px at 15% 65%, rgba(255,255,255,0.08), transparent);
  background-size: 200% 200%;
  background-repeat: repeat;
  animation: float 20s ease-in-out infinite;
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 0 24px;
  max-width: 900px;
  animation: fadeInUp 0.8s ease;
}

.hero-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 20px;
  background: rgba(102, 126, 234, 0.15);
  border: 1px solid rgba(102, 126, 234, 0.3);
  color: #667eea;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 24px;
}

.hero-title {
  font-size: 56px;
  font-weight: 800;
  line-height: 1.2;
  color: #fff;
  margin-bottom: 20px;
  letter-spacing: -1px;
}

.hero-subtitle {
  font-size: 16px;
  line-height: 1.8;
  color: #888;
  margin-bottom: 40px;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-bottom: 60px;
}

.cta-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  font-size: 16px;
  padding: 12px 32px;
  height: auto;
}

.ghost-btn {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #ccc;
  font-size: 16px;
  padding: 12px 32px;
  height: auto;
}

.ghost-btn:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.hero-stats {
  display: flex;
  gap: 48px;
  justify-content: center;
}

.hero-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-num {
  font-size: 32px;
  font-weight: 800;
  color: #fff;
}

.stat-label {
  font-size: 13px;
  color: #666;
}

/* ===== 通用 Section ===== */
.section-header {
  text-align: center;
  padding: 80px 24px 48px;
}

.section-label {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 3px;
  color: #667eea;
  margin-bottom: 12px;
}

.section-title {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
}

.section-desc {
  font-size: 15px;
  color: #666;
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
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  padding: 32px 24px;
  transition: all 0.3s ease;
}

.feature-card:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(102, 126, 234, 0.3);
  transform: translateY(-4px);
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
}

.feature-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 8px;
}

.feature-card p {
  font-size: 14px;
  color: #888;
  line-height: 1.6;
  margin-bottom: 16px;
}

.feature-list {
  list-style: none;
  padding: 0;
}

.feature-list li {
  font-size: 13px;
  color: #aaa;
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
  background: #667eea;
}

/* ===== 流程时间线 ===== */
.process-section {
  background: #080808;
  padding: 0 32px 80px;
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
  background: linear-gradient(180deg, transparent 0%, rgba(102, 126, 234, 0.3) 10%, rgba(102, 126, 234, 0.3) 90%, transparent 100%);
  transform: translateX(-50%);
}

.timeline-item {
  display: flex;
  justify-content: center;
  margin-bottom: 32px;
  position: relative;
}

.timeline-item:nth-child(odd) {
  justify-content: flex-start;
}

.timeline-item:nth-child(even) {
  justify-content: flex-end;
}

.timeline-dot {
  position: absolute;
  left: 50%;
  top: 24px;
  transform: translateX(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #0d0d0d;
  border: 2px solid #667eea;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

.dot-number {
  font-size: 14px;
  font-weight: 700;
  color: #667eea;
}

.timeline-card {
  width: 42%;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  padding: 24px;
  transition: all 0.3s ease;
}

.timeline-card:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(102, 126, 234, 0.3);
}

.timeline-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(102, 126, 234, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
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
  color: #888;
  line-height: 1.6;
  margin-bottom: 12px;
}

.timeline-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.timeline-tag {
  border-color: rgba(102, 126, 234, 0.3) !important;
  background: rgba(102, 126, 234, 0.1) !important;
  color: #667eea !important;
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
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 20px;
  padding: 32px 24px;
  text-align: center;
  position: relative;
  transition: all 0.3s ease;
}

.tier-card:hover {
  background: rgba(255, 255, 255, 0.05);
  transform: translateY(-4px);
}

.tier-card.featured {
  border-color: rgba(102, 126, 234, 0.4);
  background: rgba(102, 126, 234, 0.05);
  transform: scale(1.03);
}

.tier-card.featured:hover {
  transform: scale(1.03) translateY(-4px);
}

.tier-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  padding: 4px 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.tier-name {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 20px;
}

.tier-price {
  margin-bottom: 8px;
}

.tier-design {
  font-size: 48px;
  font-weight: 800;
  color: #667eea;
}

.tier-unit {
  font-size: 14px;
  color: #666;
  margin-left: 4px;
}

.tier-selected {
  margin-bottom: 20px;
}

.selected-num {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.selected-label {
  font-size: 13px;
  color: #888;
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
  color: #ccc;
}

.tier-btn {
  width: 100%;
}

/* ===== 状态机展示 ===== */
.status-section {
  background: #080808;
  padding: 0 32px 80px;
  overflow-x: auto;
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

.status-node {
  display: flex;
  align-items: center;
}

.status-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid;
  border-radius: 12px;
  min-width: 80px;
  transition: all 0.3s ease;
}

.status-card:hover {
  background: rgba(255, 255, 255, 0.06);
  transform: translateY(-2px);
}

.status-name {
  font-size: 12px;
  color: #ccc;
  white-space: nowrap;
}

.status-arrow {
  color: #333;
  margin: 0 4px;
  flex-shrink: 0;
}

/* ===== CTA ===== */
.cta-section {
  padding: 80px 32px;
  text-align: center;
  background:
    radial-gradient(ellipse at center, rgba(102, 126, 234, 0.1) 0%, transparent 70%);
}

.cta-content {
  max-width: 600px;
  margin: 0 auto;
}

.cta-title {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 16px;
}

.cta-desc {
  font-size: 15px;
  color: #888;
  margin-bottom: 32px;
}

.cta-final-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  font-size: 16px;
  padding: 14px 40px;
  height: auto;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .hero-title { font-size: 32px; }
  .features-grid { grid-template-columns: 1fr; }
  .tier-grid { grid-template-columns: 1fr; }
  .tier-card.featured { transform: none; }
  .tier-card.featured:hover { transform: translateY(-4px); }
  .timeline-line { left: 20px; }
  .timeline-item:nth-child(odd),
  .timeline-item:nth-child(even) { justify-content: flex-start; }
  .timeline-dot { left: 20px; }
  .timeline-card { width: calc(100% - 60px); margin-left: 60px; }
  .nav-center { display: none; }
  .hero-stats { gap: 24px; }
  .stat-num { font-size: 24px; }
  .status-flow { flex-direction: column; }
  .status-arrow { transform: rotate(90deg); }
}
</style>
