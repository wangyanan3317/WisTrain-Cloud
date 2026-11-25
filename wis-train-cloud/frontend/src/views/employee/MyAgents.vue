<template>
  <div>
    <div class="pagehead">
      <h1>我的智能体</h1>
      <div class="meta mono">MY_AGENTS · {{ unlockedCount }}/{{ list.length }} 解锁 · 服务端实时判定</div>
    </div>

    <el-empty v-if="!list.length" description="加入培训项目并完成学习后，将解锁配套智能体" />

    <!-- 卡片网格：记忆点 = 解锁状态灯（信号橙呼吸） -->
    <section class="agents">
      <div v-for="a in list" :key="a.agentId + '-' + a.projectId" class="agent" :class="{ locked: !a.unlocked || !a.enabled }">
        <div class="head">
          <span class="lamp" :class="{ on: a.unlocked && a.enabled }" aria-hidden="true"></span>
          <span class="nm">{{ a.agentName }}</span>
          <span class="pf mono">{{ a.platformName }}</span>
          <span v-if="!a.enabled" class="pf mono off">停用</span>
        </div>
        <p>{{ a.agentDesc || '暂无简介' }}</p>
        <div class="from mono">来自项目：{{ a.projectName }} · 进度 {{ a.progress }}%</div>
        <button class="go mono" :disabled="!a.unlocked || !a.enabled" @click="enter(a)">
          {{ a.enabled ? (a.unlocked ? '进入会话 →' : '未解锁 · ' + a.progress + '%') : '已停用' }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { agentApi } from '../../api/modules'

const router = useRouter()
const list = ref([])
const unlockedCount = computed(() => list.value.filter(a => a.unlocked && a.enabled).length)

function enter(a) {
  router.push(`/workspace/agents/${a.agentId}/chat`)
}

onMounted(async () => {
  list.value = await agentApi.my()
})
</script>

<style scoped>
@import '../../styles/tokens.css';

.pagehead { display: flex; align-items: flex-end; gap: 24px; padding: 8px 0 16px; border-bottom: 3px solid var(--line-strong); }
h1 { font-size: var(--fs-display); font-weight: 900; letter-spacing: -.02em; line-height: 1; }
.meta { margin-left: auto; font-size: 11px; color: var(--text-3); }

.agents { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; margin-top: 24px; }
.agent {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  padding: 16px;
  transition: transform .18s var(--ease), box-shadow .18s var(--ease);
}
.agent:hover { transform: translateY(-2px); box-shadow: var(--sh-2); }
.head { display: flex; align-items: center; gap: 8px; }
.lamp { width: 9px; height: 9px; border-radius: 50%; background: var(--text-3); flex: 0 0 auto; }
.lamp.on { background: var(--signal); box-shadow: 0 0 0 3px rgba(255, 107, 44, .18); animation: blink 1.8s infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: .3; } }
.nm { font-weight: 700; }
.pf { font-size: 10px; border: 1px solid var(--line); padding: 1px 6px; color: var(--text-3); margin-left: auto; }
.pf.off { color: var(--danger); border-color: currentColor; }
.agent p { font-size: 12px; color: var(--text-2); margin: 10px 0; line-height: 1.7; min-height: 40px; }
.from { font-size: 11px; color: var(--text-3); margin-bottom: 12px; }
.go {
  width: 100%; font-size: 12px; padding: 9px 14px; min-height: 36px;
  border: 1px solid var(--text); background: var(--text); color: #fff;
  border-radius: var(--r); cursor: pointer; letter-spacing: .04em;
}
.go:hover:not(:disabled) { filter: brightness(1.1); }
.go:disabled { background: transparent; color: var(--text-3); border-color: var(--line); cursor: not-allowed; }
.agent:not(.locked) .go:not(:disabled) { background: var(--signal); border-color: var(--signal); font-weight: 700; }

@media (prefers-reduced-motion: reduce) { .lamp.on { animation: none; } }
</style>
