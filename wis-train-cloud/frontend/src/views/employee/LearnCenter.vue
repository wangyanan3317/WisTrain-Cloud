<template>
  <div>
    <div class="pagehead">
      <h1>学习中心</h1>
      <div class="meta mono">PROJECT #{{ projectId }} · 已学 {{ learned }}/{{ total }} · 进度 {{ progress }}%</div>
    </div>

    <!-- 进度条 -->
    <div class="bar-wrap">
      <div class="bar"><i :class="{ done: progress >= 100 }" :style="{ '--w': progress + '%' }"></i></div>
      <span class="mono hint">完成全部资料后自动解锁配套智能体（信号橙点亮）</span>
    </div>

    <el-empty v-if="!list.length" description="该项目暂无资料" />

    <!-- 资料列表（US-303）：按学习顺序 -->
    <section class="materials">
      <div v-for="m in list" :key="m.materialId" class="mat" :class="{ learned: m.learned }">
        <span class="idx mono">{{ String(m.materialOrder + 1).padStart(2, '0') }}</span>
        <div class="info">
          <div class="nm">
            <el-tag size="small" class="mono" :type="typeTag(m.materialType)">{{ typeText(m.materialType) }}</el-tag>
            {{ m.materialName }}
          </div>
          <div class="url mono">{{ m.materialUrl }}</div>
        </div>
        <div class="ops">
          <el-link v-if="m.materialType === 3" :href="m.materialUrl" target="_blank" class="mono">打开外链 ↗</el-link>
          <el-button size="small" class="mono" :type="m.learned ? 'success' : 'primary'" :disabled="m.learned" @click="finish(m)">
            {{ m.learned ? '✓ 已学 ' + timeText(m.learnTime) : '标记已学' }}
          </el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { projectApi } from '../../api/modules'

const route = useRoute()
const projectId = route.params.projectId
const list = ref([])

const total = computed(() => list.value.length)
const learned = computed(() => list.value.filter(m => m.learned).length)
const progress = computed(() => total.value === 0 ? 0 : Math.round(learned.value * 100 / total.value))

function typeText(t) { return { 1: '文档', 2: '视频', 3: '外链' }[t] || '资料' }
function typeTag(t) { return { 1: 'info', 2: 'warning', 3: 'primary' }[t] || 'info' }
function timeText(t) { return t ? t.slice(5, 10) : '' }

/** 标记已学 → 后端重算进度 + 权限联动；返回最新项目视图更新进度条 */
async function finish(m) {
  const vo = await projectApi.finish(m.materialId)
  m.learned = true
  ElMessage.success(vo && vo.unlockTime && !m._celebrated
    ? '🎉 学习进度已更新，配套智能体已解锁！'
    : '已标记完成')
}

onMounted(async () => {
  list.value = await projectApi.materials(projectId)
})
</script>

<style scoped>
@import '../../styles/tokens.css';

.pagehead { display: flex; align-items: flex-end; gap: 24px; padding: 8px 0 16px; border-bottom: 3px solid var(--line-strong); }
h1 { font-size: 34px; font-weight: 900; letter-spacing: -.02em; line-height: 1; }
.meta { margin-left: auto; font-size: 11px; color: var(--text-3); }

.bar-wrap { margin: 20px 0; }
.bar { height: 6px; background: var(--surface-2); position: relative; overflow: hidden; border-radius: var(--r); }
.bar i { position: absolute; inset: 0; width: var(--w); background: var(--accent); transition: width .6s var(--ease); }
.bar i.done { background: var(--ok); }
.hint { font-size: 11px; color: var(--text-3); margin-top: 8px; display: block; }

.materials { background: var(--surface); border: 1px solid var(--line); border-radius: var(--r); }
.mat { display: flex; align-items: center; gap: 14px; padding: 14px 20px; border-bottom: 1px solid var(--line); transition: background var(--dur-fast) ease-out; }
.mat:hover { background: var(--surface-2); }
.mat.learned { opacity: .75; }
.idx { font-size: 16px; font-weight: 700; color: var(--text-3); width: 32px; }
.info { flex: 1; min-width: 0; }
.nm { font-weight: 600; }
.url { font-size: 11px; color: var(--text-3); margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ops { display: flex; gap: 12px; align-items: center; }
</style>
