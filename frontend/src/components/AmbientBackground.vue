<template>
  <!-- 全局液态氛围背景：流动渐变光斑 + 网格 + 噪点 + 飘浮粒子 -->
  <div class="ambient-bg" aria-hidden="true">
    <div class="ambient-blob blob-1"></div>
    <div class="ambient-blob blob-2"></div>
    <div class="ambient-blob blob-3"></div>
    <div class="ambient-blob blob-4"></div>
    <div class="ambient-grid"></div>
    <div class="ambient-noise"></div>
    <div v-if="particles" class="ambient-particles">
      <span v-for="i in 18" :key="i" class="particle" :style="particleStyle(i)"></span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  // 是否显示飘浮粒子（首页等营销页开启，内页可关闭以降低干扰）
  particles: { type: Boolean, default: true }
})

function particleStyle(i) {
  const seed = (i * 137.5) % 100
  const size = 2 + (i % 3)
  const left = (seed * 0.97) % 100
  const duration = 14 + (i % 7) * 3
  const delay = -(i * 1.7)
  return {
    left: `${left}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDuration: `${duration}s`,
    animationDelay: `${delay}s`
  }
}
</script>

<style scoped>
.ambient-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
  background:
    radial-gradient(ellipse 80% 60% at 50% -10%, rgba(109, 124, 255, 0.08), transparent),
    var(--bg-deep, #07070e);
}

/* --- 液态光斑 --- */
.ambient-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  will-change: transform;
}

.blob-1 {
  width: 46vw;
  height: 46vw;
  min-width: 420px;
  min-height: 420px;
  top: -14%;
  left: -8%;
  background: radial-gradient(circle, rgba(109, 124, 255, 0.22) 0%, transparent 65%);
  animation: blobDrift 26s ease-in-out infinite;
}

.blob-2 {
  width: 40vw;
  height: 40vw;
  min-width: 380px;
  min-height: 380px;
  top: 30%;
  right: -12%;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.18) 0%, transparent 65%);
  animation: blobDriftAlt 32s ease-in-out infinite;
}

.blob-3 {
  width: 34vw;
  height: 34vw;
  min-width: 320px;
  min-height: 320px;
  bottom: -12%;
  left: 22%;
  background: radial-gradient(circle, rgba(236, 72, 153, 0.12) 0%, transparent 65%);
  animation: blobDrift 38s ease-in-out infinite reverse;
}

.blob-4 {
  width: 24vw;
  height: 24vw;
  min-width: 240px;
  min-height: 240px;
  top: 55%;
  left: 45%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.08) 0%, transparent 65%);
  animation: blobDriftAlt 30s ease-in-out infinite;
}

/* --- 细网格 --- */
.ambient-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.022) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.022) 1px, transparent 1px);
  background-size: 72px 72px;
  mask-image: radial-gradient(ellipse 90% 70% at 50% 40%, rgba(0, 0, 0, 0.7), transparent 100%);
  -webkit-mask-image: radial-gradient(ellipse 90% 70% at 50% 40%, rgba(0, 0, 0, 0.7), transparent 100%);
}

/* --- 噪点质感 --- */
.ambient-noise {
  position: absolute;
  inset: 0;
  opacity: 0.5;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.035'/%3E%3C/svg%3E");
  background-size: 200px 200px;
}

/* --- 飘浮粒子 --- */
.ambient-particles {
  position: absolute;
  inset: 0;
}

.particle {
  position: absolute;
  bottom: -10px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(180, 190, 255, 0.8), rgba(109, 124, 255, 0.2));
  box-shadow: 0 0 6px rgba(109, 124, 255, 0.6);
  animation-name: particleRise;
  animation-timing-function: linear;
  animation-iteration-count: infinite;
  opacity: 0;
}

@keyframes particleRise {
  0% {
    transform: translateY(0) translateX(0);
    opacity: 0;
  }
  8% {
    opacity: 0.8;
  }
  90% {
    opacity: 0.5;
  }
  100% {
    transform: translateY(-105vh) translateX(40px);
    opacity: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .ambient-blob, .particle {
    animation: none;
  }
  .particle {
    opacity: 0.3;
  }
}
</style>
