import { describe, it, expect } from 'vitest'
import { formatMoney, formatMoneyCNY } from './money.js'

describe('formatMoney', () => {
  it('整数补 .00', () => {
    expect(formatMoney(1000)).toBe('1000.00')
    expect(formatMoney(0)).toBe('0.00')
  })

  it('保留两位小数', () => {
    expect(formatMoney(1000.01)).toBe('1000.01')
    expect(formatMoney(99.9)).toBe('99.90')
  })

  it('超过两位精度时截断,避免后端精度差异导致展示抖动', () => {
    // 162.500005 → 162.50(后端 MoneyUtils 用 BigDecimal HALF_UP,前端 toFixed 等价四舍五入)
    expect(formatMoney(162.500005)).toBe('162.50')
  })

  it('非法输入兜底为 0.00 而不是 NaN', () => {
    expect(formatMoney(null)).toBe('0.00')
    expect(formatMoney(undefined)).toBe('0.00')
    expect(formatMoney('abc')).toBe('0.00')
  })

  it('接受字符串数字(后端 JSON 反序列化兼容)', () => {
    expect(formatMoney('500.00')).toBe('500.00')
  })
})

describe('formatMoneyCNY', () => {
  it('前置 ¥ 符号', () => {
    expect(formatMoneyCNY(1000)).toBe('¥1000.00')
  })
})