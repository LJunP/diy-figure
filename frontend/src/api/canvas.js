/**
 * 画布 API
 */
import request from './request'
import { handleUnauthorized } from './request'
import { consumeSseLines } from '@/utils/sse'

export { consumeSseLines }

export function createCanvas(seriesId, data) {
  return request.post(`/series/${seriesId}/canvases`, data)
}

export function getCanvasDetail(id) {
  return request.get(`/canvases/${id}`)
}

export function deleteCanvas(id) {
  return request.delete(`/canvases/${id}`)
}

export function finalizeCanvas(id) {
  return request.post(`/canvases/${id}/finalize`)
}

export function reopenCanvas(id) {
  return request.post(`/canvases/${id}/reopen`)
}

export function retryModel3d(id) {
  return request.post(`/canvases/${id}/model3d/retry`)
}

export async function chatWithAI(canvasId, data, callbacks, options = {}) {
  const token = localStorage.getItem('token')
  let finished = false
  const finish = (fn) => {
    if (finished) return
    finished = true
    fn?.()
  }
  try {
    const response = await fetch(`/api/canvases/${canvasId}/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify(data),
      signal: options.signal
    })

    if (response.status === 401) {
      handleUnauthorized()
      throw new Error('登录已过期')
    }
    if (!response.ok) {
      let message = `HTTP ${response.status}`
      try {
        const body = await response.json()
        message = body.message || message
      } catch (_) {
        /* 非 JSON */
      }
      throw new Error(message)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let currentEventType = 'message'

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        finish(() => callbacks.onDone?.())
        break
      }
      buffer += decoder.decode(value, { stream: true })
      const consumed = consumeSseLines(buffer, {
        ...callbacks,
        onDone: () => finish(() => callbacks.onDone?.())
      }, currentEventType)
      currentEventType = consumed.currentEventType
      buffer = consumed.rest
    }
  } catch (error) {
    if (error?.name === 'AbortError') {
      finish(() => {})
      return
    }
    finish(() => callbacks.onError?.(error))
  }
}

export function uploadReferenceImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/upload-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
