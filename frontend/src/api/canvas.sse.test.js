import { describe, it, expect, vi } from 'vitest'
import { consumeSseLines } from '../utils/sse.js'

describe('consumeSseLines', () => {
  it('解析 done 事件', () => {
    const onDone = vi.fn()
    consumeSseLines('event: done\ndata:\n\n', { onDone })
    expect(onDone).toHaveBeenCalledTimes(1)
  })

  it('解析图片 URL', () => {
    const onImageGenerated = vi.fn()
    consumeSseLines('event: image_generated\ndata: https://cdn.example/a.png\n\n', { onImageGenerated })
    expect(onImageGenerated).toHaveBeenCalledWith('https://cdn.example/a.png')
  })

  it('普通 token 走 onMessage', () => {
    const onMessage = vi.fn()
    consumeSseLines('data:你\n\ndata:好\n\n', { onMessage })
    expect(onMessage).toHaveBeenCalledWith('你')
    expect(onMessage).toHaveBeenCalledWith('好')
  })
})
