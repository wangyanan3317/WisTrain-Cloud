<template>
  <div>
    <!-- 页头：粗规线 + 等宽元信息 -->
    <div class="pagehead">
      <h1>我的项目</h1>
      <div class="meta mono">MY_PROJECTS · {{ list.length }} 个 · 资格实时计算</div>
    </div>

    <!-- 统计卡 -->
    <section class="stats">
      <div class="stat"><div class="k mono">进行中</div><div class="v mono">{{ activeCount }}</div></div>
      <div class="stat"><div class="k mono">已完成</div><div class="v mono">{{ doneCount }}</div></div>
      <div class="stat hot"><div class="k mono">已解锁智能体</div><div class="v mono"><em>{{ unlockedSum }}</em><small>/{{ agentSum }}</small></div></div>
    </section>

    <el-empty v-if="!list.length" description="暂无培训项目，可在下方加入" />

    <!-- 项目卡列表 -->
    <section class="proj-list">
      <div v-for="p in list" :key="p.projectId" class="proj">
        <div>
          <div class="name">{{ p.projectName }}</div>
          <div class="sub mono">MATERIALS {{ p.materialLearned }}/{{ p.materialTotal }} · {{ p.agentUnlocked }}/{{ p.agentTotal }} AGENTS · STATE={{ p.projectState }}</div>
          <div class="bar"><i :class="{ done: p.progress >= 100 }" :style="{ '--w': p.progress + '%' }"></i></div>
        </div>
        <div class="badges">
          <span class="bdg mono" :class="badgeClass(p)">{{ stateText(p) }}</span>
          <span class="bdg mono unlock" v-if="p.unlockTime">已解锁</span>
          <el-button size="small" class="mono" type="primary" text bg @click="goLearn(p)">学习 →</el-button>
        </div>
      </div>
    </section>

    <!-- 加入项目（US-302） -->
    <div class="join-row">
      <h3 class="mono">AVAILABLE · 可加入项目</h3>
      <el-empty v-if="!available.length" description="暂无可加入的项目" :image-size="60" />
      <div v-for="p in available" :key="p.projectId" class="avail">
        <div><b>{{ p.projectName }}</b><span class="sub mono"> · {{ p.projectDesc || '—' }}</span></div>
        <el-button size="small" class="mono" @click="join(p)">+ 加入</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { projectApi } from '../../api/modules'

const router = useRouter()
const list = ref([])
const available = ref([])

const activeCount = computed(() => list.value.filter(p => p.projectState === 1).length)
const doneCount = computed(() => list.value.filter(p => p.learnState === 2).length)
const unlockedSum = computed(() => list.value.reduce((s, p) => s + p.agentUnlocked, 0))
const agentSum = computed(() => list.value.reduce((s, p) => s + p.agentTotal, 0))

function badgeClass(p) {
  return p.projectState === 2 ? 'end' : p.learnState === 2 ? 'ok' : 'run'
}
function stateText(p) {
  return p.projectState === 2 ? '已结束' : p.learnState === 2 ? '已完成' : '进行中'
}
function goLearn(p) {
  router.push(`/workspace/learn/${p.projectId}`)
}

async function join(p) {
  await projectApi.join(p.projectId)
  ElMessage.success('已加入，开始学习吧')
  await load()
}

async function load() {
  list.value = await projectApi.my()
  available.value = await projectApi.available()
}

onMounted(load)
</script>

<style scoped>
@import '../../styles/tokens.css';

.pagehead { display: flex; align-items: flex-end; gap: 24px; padding: 8px 0 16px; border-bottom: 3px solid var(--line-strong); }
h1 { font-size: var(--fs-display); font-weight: 900; letter-spacing: -.02em; line-height: 1; }
.meta { margin-left: auto; font-size: 11px; color: var(--text-3); }

.stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin: 24px 0; }
.stat { background: var(--surface); border: 1px solid var(--line); border-radius: var(--r); padding: 16px 20px; position: relative; box-shadow: var(--sh-1); }
.stat::before { content: ""; position: absolute; top: 0; left: 0; width: 32px; height: 3px; background: var(--text); }
.stat.hot::before { background: var(--signal); }
.stat .k { font-size: 11px; color: var(--text-3); letter-spacing: .08em; }
.stat .v { font-size: 36px; font-weight: 700; line-height: 1.15; }
.stat .v small { font-size: 15px; color: var(--text-3); font-weight: 400; }
.stat.hot .v em { font-style: normal; color: var(--signal); }

.proj-list { background: var(--surface); border: 1px solid var(--line); border-radius: var(--r); }
.proj { display: grid; grid-template-columns: 1fr auto; gap: 12px; padding: 16px 20px; border-bottom: 1px solid var(--line); align-items: start; transition: background var(--dur-fast) ease-out; }
.proj:hover { background: var(--surface-2); }
.name { font-weight: 700; font-size: 15px; }
.sub { font-size: 12px; color: var(--text-3); margin-top: 2px; }
.bar { height: 4px; background: var(--surface-2); margin-top: 10px; position: relative; overflow: hidden; }
.bar i { position: absolute; inset: 0; width: var(--w); background: var(--accent); transition: width .6s var(--ease); }
.bar i.done { background: var(--ok); }
.badges { display: flex; gap: 6px; align-items: center; }
.bdg { font-size: 11px; padding: 3px 8px; border: 1px solid var(--line); color: var(--text-2); white-space: nowrap; }
.bdg.ok { color: var(--ok); border-color: currentColor; }
.bdg.run { color: var(--accent-deep); border-color: currentColor; }
.bdg.end { color: var(--text-3); }
.bdg.unlock { background: var(--signal); border-color: var(--signal); color: #fff; font-weight: 700; }

.join-row { margin-top: 24px; }
.join-row h3 { font-size: 12px; color: var(--text-2); letter-spacing: .08em; margin-bottom: 10px; }
.avail { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; background: var(--surface); border: 1px solid var(--line); border-radius: var(--r); margin-bottom: 8px; }
.avail .sub { font-size: 12px; color: var(--text-3); }
</style>
