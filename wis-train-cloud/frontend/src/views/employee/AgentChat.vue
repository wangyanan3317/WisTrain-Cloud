<template>
  <div class="chat-page">
    <!-- 会话侧栏 -->
    <aside class="sessions">
      <div class="s-head mono">SESSIONS · {{ agentName }}</div>
      <el-button size="small" class="mono new" @click="newSession">+ 新会话</el-button>
      <div v-for="s in sessions" :key="s.sessionId" class="s-item mono" :class="{ active: s.sessionId === sessionId }" @click="switchSession(s)">
        {{ s.sessionTitle }}
      </div>
    </aside>

    <!-- 对话主区 -->
    <main class="chat-main">
      <div class="c-head">
        <h1>{{ agentName }}</h1>
        <span class="mono hint">SSE STREAM · 服务端实时校验解锁资格</span>
      </div>

      <div ref="scrollRef" class="msgs">
        <el-empty v-if="!msgs.length" description="输入问题开始对话（US-405 多轮会话）" />
        <div v-for="(m, i) in msgs" :key="i" class="msg" :class="m.role">
          <span class="tag mono">{{ m.role === 'user' ? 'YOU' : 'AGENT' }}</span>
          <div class="bubble">{{ m.text }}<i v-if="m.typing" class="caret mono">▌</i></div>
        </div>
      </div>

      <div class="input-row">
        <el-input v-model="input" size="large" placeholder="输入消息，Enter 发送…" :disabled="sending" @keyup.enter="send" />
        <el-button size="large" class="mono" type="warning" :loading="sending" @click="send">{{ sending ? '生成中…' : '发送 →' }}</el-button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { agentApi } from '../../api/modules'

const route = useRoute()
const agentId = route.params.agentId
const agentName = ref('智能体会话')
const sessions = ref([])
const sessionId = ref(null)
const msgs = ref([])
const input = ref('')
const sending = ref(false)
const scrollRef = ref()

function scrollToBottom() {
  nextTick(() => { scrollRef.value && (scrollRef.value.scrollTop = scrollRef.value.scrollHeight) })
}

function newSession() {
  sessionId.value = null
  msgs.value = []
}

function switchSession(s) {
  sessionId.value = s.sessionId
  msgs.value = [] // 历史消息明细表为选做模块，此处切换仅定位会话
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  msgs.value.push({ role: 'user', text })
  input.value = ''
  sending.value = true
  const reply = { role: 'agent', text: '', typing: true }
  msgs.value.push(reply)
  scrollToBottom()

  try {
    // SSE 流式接收（fetch + ReadableStream），携带多轮上下文（不含本轮）
    const history = msgs.value
      .slice(0, -2)
      .filter(m => m.text && !m.typing)
      .slice(-10)
      .map(m => ({ role: m.role === 'user' ? 'user' : 'assistant', content: m.text }))
    const sid = await agentApi.chatStream(agentId, {
      sessionId: sessionId.value,
      message: text,
      history,
      onChunk: chunk => {
        reply.text += chunk
        scrollToBottom()
      },
      onError: msg => ElMessage.error(msg || '调用失败')
    })
    if (sid) sessionId.value = sid
    reply.typing = false
  } catch {
    reply.typing = false
    if (!reply.text) reply.text = '（调用失败，请重试）'
  } finally {
    sending.value = false
    scrollToBottom()
    loadSessions()
  }
}

async function loadSessions() {
  sessions.value = await agentApi.sessions(agentId)
}

onMounted(async () => {
  await loadSessions()
  if (sessions.value.length) {
    sessionId.value = sessions.value[0].sessionId
    agentName.value = sessions.value[0].sessionTitle || '智能体会话'
  }
})
</script>

<style scoped>
@import '../../styles/tokens.css';

.chat-page { display: grid; grid-template-columns: 240px 1fr; gap: 16px; height: calc(100vh - 100px); }

.sessions { background: var(--surface); border: 1px solid var(--line); border-radius: var(--r); padding: 12px; overflow: auto; }
.s-head { font-size: 11px; color: var(--text-3); letter-spacing: .08em; margin-bottom: 10px; }
.new { width: 100%; margin-bottom: 10px; }
.s-item { font-size: 12px; padding: 8px 10px; border-radius: var(--r); cursor: pointer; color: var(--text-2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.s-item:hover { background: var(--surface-2); }
.s-item.active { background: var(--dark); color: #fff; }

.chat-main { display: flex; flex-direction: column; background: var(--surface); border: 1px solid var(--line); border-radius: var(--r); min-width: 0; }
.c-head { padding: 14px 20px; border-bottom: 1px solid var(--line); display: flex; align-items: baseline; gap: 12px; }
.c-head h1 { font-size: 20px; font-weight: 800; letter-spacing: -.02em; }
.hint { font-size: 11px; color: var(--text-3); }

.msgs { flex: 1; overflow: auto; padding: 20px; }
.msg { display: flex; gap: 10px; margin-bottom: 14px; align-items: flex-start; }
.msg.user { flex-direction: row-reverse; }
.tag { font-size: 10px; color: var(--text-3); border: 1px solid var(--line); padding: 1px 6px; margin-top: 4px; }
.msg.user .tag { color: var(--accent-deep); border-color: currentColor; }
.bubble {
  max-width: 72%;
  padding: 10px 14px;
  border: 1px solid var(--line);
  border-radius: var(--r);
  font-size: 13px; line-height: 1.7;
  white-space: pre-wrap; word-break: break-word;
  background: var(--surface-2);
}
.msg.user .bubble { background: var(--dark); color: #F5F1EA; }
.msg.agent .bubble { border-left: 3px solid var(--signal); }
.caret { animation: blink 1s infinite; color: var(--signal); }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: .2; } }

.input-row { display: flex; gap: 12px; padding: 14px 20px; border-top: 1px solid var(--line); }
.input-row .el-input { flex: 1; }

@media (max-width: 768px) { .chat-page { grid-template-columns: 1fr; } .sessions { display: none; } }
</style>
