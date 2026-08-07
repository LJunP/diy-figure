<template>
  <div class="not-found">
    <div class="nf-content">
      <div class="nf-code">404</div>
      <h1 class="nf-title">页面不存在</h1>
      <p class="nf-desc">你访问的页面可能已被移除或暂时不可用</p>
      <el-button type="primary" round size="large" @click="$router.push('/')">
        返回首页
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0d0d0d;
}

.nf-content {
  text-align: center;
}

.nf-code {
  font-size: 120px;
  font-weight: 900;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  margin-bottom: 16px;
}

.nf-title {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
}

.nf-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 32px;
}
</style>
