/**
 * 画布 API
 */
import request from './request'

/** 在系列下创建画布 */
export function createCanvas(seriesId, data) {
  return request.post(`/series/${seriesId}/canvases`, data)
}

/** 画布详情 */
export function getCanvasDetail(id) {
  return request.get(`/canvases/${id}`)
}

/** 删除画布 */
export function deleteCanvas(id) {
  return request.delete(`/canvases/${id}`)
}

/** 画布定稿(触发 3D 模型生成) */
export function finalizeCanvas(id) {
  return request.post(`/canvases/${id}/finalize`)
}

/** 重新打开已定稿画布 */
export function reopenCanvas(id) {
  return request.post(`/canvases/${id}/reopen`)
}

/**
 * AI 对话(SSE 流式)
 * 使用 fetch + ReadableStream 接收 SSE 事件
 *
 * @param {number} canvasId 画布 ID
 * @param {Object} data { message, imageUrls, generateImage }
 * @param {Object} callbacks { onMessage, onImageGenerating, onImageGenerated, onImageError, onDone, onError }
 */
export async function chatWithAI(canvasId, data, callbacks) {
  const token = localStorage.getItem('token')
  try {
    const response = await fetch(`/api/canvases/${canvasId}/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(data)
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    let currentEventType = 'message'

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        // 流结束,触发 onDone 回调
        callbacks.onDone?.()
        break
      }

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

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

          // 根据事件类型和内容判断
          if (currentEventType === 'image_generating' || dataStr === '正在生成概念图...') {
            callbacks.onImageGenerating?.()
          } else if (currentEventType === 'image_generated' || (dataStr.startsWith('http') && currentEventType !== 'message')) {
            const urlMatch = dataStr.match(/https?:\/\/[^\s"']+/)
            if (urlMatch) {
              callbacks.onImageGenerated?.(urlMatch[0])
            } else {
              callbacks.onImageGenerated?.(dataStr)
            }
          } else if (currentEventType === 'image_error') {
            callbacks.onImageError?.(dataStr)
          } else if (currentEventType === 'done') {
            // 后端显式发送 done 事件
            callbacks.onDone?.()
          } else if (currentEventType === 'error') {
            callbacks.onError?.(new Error(dataStr))
          } else {
            // 普通文本消息(token)
            callbacks.onMessage?.(dataStr)
          }
          currentEventType = 'message'
        }
      }
    }
  } catch (error) {
    callbacks.onError?.(error)
  }
}

/** 上传参考图 */
export function uploadReferenceImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/upload-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
