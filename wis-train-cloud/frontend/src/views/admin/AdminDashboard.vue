<template>
  <div>
    <div class="pagehead">
      <h1>工作台</h1>
      <div class="meta mono">ADMIN.DASHBOARD · 近 7 日趋势</div>
    </div>

    <!-- 统计卡：等宽大数字 -->
    <section class="stats">
      <div class="stat"><div class="k mono">项目总数</div><div class="v mono">{{ s.projectCount ?? '-' }}</div></div>
      <div class="stat"><div class="k mono">进行中</div><div class="v mono">{{ s.activeProjects ?? '-' }}</div></div>
      <div class="stat"><div class="k mono">部门员工</div><div class="v mono">{{ s.employeeCount ?? '-' }}</div></div>
      <div class="stat"><div class="k mono">培训资料</div><div class="v mono">{{ s.materialCount ?? '-' }}</div></div>
      <div class="stat hot"><div class="k mono">智能体</div><div class="v mono"><em>{{ s.agentCount ?? '-' }}</em></div></div>
      <div class="stat"><div class="k mono">7 日会话</div><div class="v mono">{{ s.sessionCount7d ?? '-' }}</div></div>
    </section>

    <!-- 近 7 日趋势：纯 CSS 柱状图（等宽数字 + 发丝网格） -->
    <section class="panel">
      <h2 class="mono">TREND · 近 7 日（加入 / 会话）</h2>
      <div class="chart">
        <div v-for="d in trend" :key="d.day" class="col">
          <div class="bars">
            <i class="b1" :style="{ height: h(d.joins) }" :title="'加入 ' + d.joins"></i>
            <i class="b2" :style="{ height: h(d.chats) }" :title="'会话 ' + d.chats"></i>
          </div>
          <span class="day mono">{{ d.day.slice(5) }}</span>
          <span class="num mono">{{ d.joins }} / {{ d.chats }}</span>
        </div>
      </div>
      <div class="legend mono"><i class="b1"></i> 加入人数 <i class="b2"></i> 会话数</div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../../api/modules'

const s = ref({})
const trend = ref([])
const max = ref(1)

function h(v) { return Math.max(4, Math.round(v * 120 / max.value)) + 'px' }

onMounted(async () => {
  s.value = await adminApi.statsSummary()
  trend.value = await adminApi.statsTrend()
  max.value = Math.max(1, ...trend.value.flatMap(d => [d.joins, d.chats]))
})
</script>

<style scoped>
@import '../../styles/tokens.css';

.pagehead { display: flex; align-items: flex-end; gap: 24px; padding: 8px 0 16px; border-bottom: 3px solid var(--line-strong); }
h1 { font-size: var(--fs-display); font-weight: 900; letter-spacing: -.02em; line-height: 1; }
.meta { margin-left: auto; font-size: 11px; color: var(--text-3); }

.stats { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 16px; margin: 24px 0; }
.stat { background: var(--surface); border: 1px solid var(--line); border-radius: var(--r); padding: 16px 20px; position: relative; box-shadow: var(--sh-1); }
.stat::before { content: ""; position: absolute; top: 0; left: 0; width: 32px; height: 3px; background: var(--text); }
.stat.hot::before { background: var(--signal); }
.stat .k { font-size: 11px; color: var(--text-3); letter-spacing: .08em; }
.stat .v { font-size: 34px; font-weight: 700; line-height: 1.15; }
.stat.hot .v em { font-style: normal; color: var(--signal); }

.panel { background: var(--surface); border: 1px solid var(--line); border-radius: var(--r); }
.panel h2 { font-size: 12px; color: var(--text-2); letter-spacing: .08em; padding: 14px 20px; border-bottom: 1px solid var(--line); }
.chart { display: flex; justify-content: space-around; align-items: flex-end; padding: 28px 20px 10px; min-height: 190px; }
.col { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.bars { display: flex; align-items: flex-end; gap: 4px; height: 124px; }
.bars i { width: 16px; transition: height .5s var(--ease); }
.b1 { background: var(--accent); }
.b2 { background: var(--text); }
.day { font-size: 11px; color: var(--text-3); }
.num { font-size: 10px; color: var(--text-2); }
.legend { padding: 12px 20px 16px; font-size: 11px; color: var(--text-3); display: flex; gap: 8px; align-items: center; }
.legend i { width: 12px; height: 12px; display: inline-block; margin: 0 6px 0 10px; }
</style>
