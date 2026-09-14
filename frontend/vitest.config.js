import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    // 单元测试为主,前端纯逻辑基本不需要 DOM;这里用 happy-dom 以备有需要
    environment: 'happy-dom',
    globals: true,
    include: ['src/**/*.{test,spec}.{js,mjs,ts}'],
    // 不让 build/vite 性能噪音盖过测试输出
    reporters: ['default'],
  },
})