/**
 * 金额格式化(前端)
 *
 * 与后端 MoneyUtils 的对应关系:
 * - 后端用 BigDecimal 保证精度
 * - 前端只是展示,toFixed(2) 即可
 */
export function formatMoney(amount) {
  const n = typeof amount === 'number' ? amount : Number(amount)
  if (!Number.isFinite(n)) return '0.00'
  return (Math.round(n * 100) / 100).toFixed(2)
}

export function formatMoneyCNY(amount) {
  return '¥' + formatMoney(amount)
}

/** 与后端 MoneyUtils.half 对齐的展示用一半 */
export function halfMoney(amount) {
  const n = typeof amount === 'number' ? amount : Number(amount)
  if (!Number.isFinite(n)) return '0.00'
  return formatMoney(Math.round(n * 50) / 100)
}
