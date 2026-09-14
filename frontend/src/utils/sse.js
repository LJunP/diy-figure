/**
 * 把 SSE 缓冲拆成事件。无 axios/router 依赖,方便单测。
 */
export function consumeSseLines(buffer, callbacks, currentEventType = 'message') {
  const lines = buffer.split('\n')
  const rest = lines.pop() || ''
  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) {
      currentEventType = 'message'
      continue
    }
    if (trimmed.startsWith('event:')) {
      currentEventType = trimmed.substring(6).trim()
      continue
    }
    if (trimmed.startsWith('data:')) {
      const dataStr = trimmed.substring(5)
      if (currentEventType === 'image_generating' || dataStr === '正在生成概念图...') {
        callbacks.onImageGenerating?.()
      } else if (currentEventType === 'image_generated' || (dataStr.startsWith('http') && currentEventType !== 'message')) {
        const urlMatch = dataStr.match(/https?:\/\/[^\s"']+/)
        callbacks.onImageGenerated?.(urlMatch ? urlMatch[0] : dataStr)
      } else if (currentEventType === 'image_error') {
        callbacks.onImageError?.(dataStr)
      } else if (currentEventType === 'done') {
        callbacks.onDone?.()
      } else if (currentEventType === 'error') {
        callbacks.onError?.(new Error(dataStr))
      } else {
        callbacks.onMessage?.(dataStr)
      }
      currentEventType = 'message'
    }
  }
  return { currentEventType, rest }
}
