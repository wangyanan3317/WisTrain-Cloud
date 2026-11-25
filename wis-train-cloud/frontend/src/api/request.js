import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

/** Axios 实例：统一 token 注入 / 401 跳转 / 响应解包 */
const request = axios.create({ baseURL: '/api', timeout: 20000 })

request.interceptors.request.use(cfg => {
  const token = localStorage.getItem('wt_token')
  if (token) cfg.headers.Authorization = 'Bearer ' + token
  return cfg
})

request.interceptors.response.use(
  resp => {
    const body = resp.data
    if (body.code !== 0) {
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(body)
    }
    return body.data
  },
  err => {
    const status = err.response?.status
    const body = err.response?.data
    if (status === 401) {
      ['wt_token', 'wt_userId', 'wt_realname', 'wt_role', 'wt_deptId'].forEach(k => localStorage.removeItem(k))
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(body?.message || err.message || '网络异常')
    }
    return Promise.reject(err)
  }
)

/**
 * SSE 流式对话（US-405）：EventSource 不支持 POST/自定义头，
 * 用 fetch + ReadableStream 手动解析 SSE data 行。
 * onChunk(text) 逐片回调；返回 Promise（结束时 resolve sessionId）。
 */
export async function chatStream(agentId, { sessionId, message, history, onChunk, onError }) {
  const resp = await fetch(`/api/agents/${agentId}/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + localStorage.getItem('wt_token')
    },
    body: JSON.stringify({ sessionId, message, history })
  })
  const reader = resp.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  return new Promise((resolve, reject) => {
    const pump = async () => {
      try {
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          // SSE 以空行分隔事件
          const events = buffer.split('\n\n')
          buffer = events.pop()
          for (const raw of events) {
            const dataLines = raw.split('\n').filter(l => l.startsWith('data:')).map(l => l.slice(5).trim())
            if (!dataLines.length) continue
            const data = dataLines.join('')
            const nameMatch = raw.match(/event:(.+)/)
            const name = nameMatch ? nameMatch[1].trim() : 'message'
            if (name === 'message') onChunk(data)
            else if (name === 'error') { onError?.(data.split('#')[0]); reject(new Error(data)) }
            else if (name === 'done') resolve(data)
          }
        }
        resolve(null)
      } catch (e) {
        onError?.(e.message)
        reject(e)
      }
    }
    pump()
  })
}

export default request
