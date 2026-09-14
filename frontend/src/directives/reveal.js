/**
 * v-reveal 滚动入场指令
 *
 * 用法:
 *   <div v-reveal>内容</div>
 *   <div v-reveal="0.15">延迟 0.15s 入场</div>
 *   <div v-reveal="{ delay: 0.2, index: 2 }">按序号交错(每个 +0.08s)</div>
 *
 * 原理: IntersectionObserver 监听进入视口,为元素添加 .reveal-visible 类,
 * 入场动画样式定义在 assets/main.css 的 .reveal 规则中。
 */

const observers = new WeakMap()

function applyReveal(el, binding) {
  el.classList.add('reveal')

  let delay = 0
  const value = binding.value
  if (typeof value === 'number') {
    delay = value
  } else if (value && typeof value === 'object') {
    delay = (value.delay || 0) + (value.index ? value.index * 0.08 : 0)
  }
  el.style.setProperty('--reveal-delay', `${delay}s`)

  if (!('IntersectionObserver' in window)) {
    el.classList.add('reveal-visible')
    return
  }

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('reveal-visible')
          observer.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.12, rootMargin: '0px 0px -40px 0px' }
  )

  observer.observe(el)
  observers.set(el, observer)
}

export default {
  mounted: applyReveal,
  updated: applyReveal,
  unmounted(el) {
    const observer = observers.get(el)
    if (observer) {
      observer.disconnect()
      observers.delete(el)
    }
  }
}
