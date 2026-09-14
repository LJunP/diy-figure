<template>
  <div class="not-found">
    <AmbientBackground />
    <div class="nf-content">
      <div class="nf-orb"></div>
      <div class="nf-code">404</div>
      <h1 class="nf-title">页面不存在</h1>
      <p class="nf-desc">你访问的页面可能已被移除或暂时不可用</p>
      <el-button round size="large" class="nf-btn" @click="$router.push('/')">
        <el-icon class="el-icon--left"><HomeFilled /></el-icon>
        返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { HomeFilled } from '@element-plus/icons-vue'
import AmbientBackground from '@/components/AmbientBackground.vue'
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
  overflow: hidden;
}

.nf-content {
  text-align: center;
  position: relative;
  animation: fadeInUp 0.8s var(--ease-out);
}

.nf-orb {
  position: absolute;
  top: -40%; left: 50%;
  transform: translateX(-50%);
  width: 480px; height: 480px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(109, 124, 255, 0.2), transparent 60%);
  filter: blur(60px);
  z-index: -1;
}

.nf-code {
  font-size: 140px;
  font-weight: 900;
  background: var(--gradient-brand);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  margin-bottom: 16px;
  animation: gradientShift 6s ease infinite;
}

.nf-title {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
}

.nf-desc {
  font-size: 14px;
  color: var(--text-3);
  margin-bottom: 32px;
}

.nf-btn {
  background: var(--gradient-brand) !important;
  border: none !important;
  color: #fff !important;
  box-shadow: 0 4px 20px rgba(109, 124, 255, 0.35);
}
</style>
