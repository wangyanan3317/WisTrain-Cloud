<template>
  <div v-if="projectId">
    <div class="pagehead">
      <h1>项目详情</h1>
      <div class="meta mono">PROJECT #{{ projectId }} · 资料与成员 · 权限关联</div>
    </div>

    <!-- 统计概览（US-205） -->
    <section class="stats">
      <div class="stat"><div class="k mono">成员数</div><div class="v mono">{{ stats.memberCount ?? '-' }}</div></div>
      <div class="stat"><div class="k mono">资料数</div><div class="v mono">{{ stats.materialCount ?? '-' }}</div></div>
      <div class="stat"><div class="k mono">平均进度</div><div class="v mono">{{ stats.avgProgress ?? '-' }}<small>%</small></div></div>
      <div class="stat hot"><div class="k mono">已完成</div><div class="v mono"><em>{{ stats.completedCount ?? '-' }}</em></div></div>
    </section>

    <el-tabs v-model="tab" class="tabs">
      <!-- 资料管理（US-203） -->
      <el-tab-pane label="培训资料" name="materials">
        <div class="row-ops">
          <el-input v-model="mat.materialName" class="mono" placeholder="资料名称" style="width: 220px" />
          <el-select v-model="mat.materialType" style="width: 110px">
            <el-option :value="1" label="文档" /><el-option :value="2" label="视频" /><el-option :value="3" label="外链" />
          </el-select>
          <el-input v-model="mat.materialUrl" class="mono" placeholder="资料地址（文件路径 / 外链 URL）" style="width: 320px" />
          <el-button class="mono" type="warning" @click="addMaterial">+ 添加</el-button>
          <span class="hint mono">文件上传通道预留，当前支持地址录入</span>
        </div>
        <el-table :data="materials" class="tbl">
          <el-table-column label="#" width="56" class-name="mono">
            <template #default="{ $index }">{{ String($index + 1).padStart(2, '0') }}</template>
          </el-table-column>
          <el-table-column prop="materialName" label="名称" min-width="160" />
          <el-table-column label="类型" width="80">
            <template #default="{ row }"><span class="mono">{{ { 1: '文档', 2: '视频', 3: '外链' }[row.materialType] }}</span></template>
          </el-table-column>
          <el-table-column prop="materialUrl" label="地址" min-width="220" class-name="mono" show-overflow-tooltip />
          <el-table-column label="操作" width="150">
            <template #default="{ row, $index }">
              <el-button size="small" text :disabled="$index === 0" class="mono" @click="move($index, -1)">↑</el-button>
              <el-button size="small" text :disabled="$index === materials.length - 1" class="mono" @click="move($index, 1)">↓</el-button>
              <el-button size="small" text type="danger" class="mono" @click="delMaterial(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 成员管理（US-204 / US-502） -->
      <el-tab-pane :label="'成员（' + members.length + '）'" name="members">
        <div class="row-ops">
          <el-select v-model="pickUser" multiple filterable placeholder="选择员工批量分配" style="width: 380px">
            <el-option v-for="u in users" :key="u.userId" :value="u.userId" :label="u.realname + '（' + u.logname + '）'" />
          </el-select>
          <el-button class="mono" type="warning" @click="assign">分配</el-button>
        </div>
        <el-table :data="members" class="tbl">
          <el-table-column prop="realname" label="姓名" width="120" />
          <el-table-column prop="logname" label="登录名" class-name="mono" min-width="140" />
          <el-table-column label="进度" width="180">
            <template #default="{ row }">
              <div class="bar"><i :style="{ '--w': row.progress + '%' }"></i></div>
            </template>
          </el-table-column>
          <el-table-column label="解锁" width="110">
            <template #default="{ row }">
              <span class="bdg mono" :class="row.unlockTime ? 'unlock' : 'lock'">{{ row.unlockTime ? '已解锁' : '未解锁' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button size="small" text type="danger" class="mono" @click="removeMember(row)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 权限关联配置（US-403） -->
      <el-tab-pane label="权限关联" name="agents">
        <div class="hint-block mono">为项目勾选可用智能体并设置解锁规则：① 完成全部资料；② 达到进度阈值。保存后立即生效（实时判定）。</div>
        <div v-for="(cfg, i) in config" :key="i" class="cfg">
          <el-checkbox v-model="cfg.on" :label="cfg.agentName" />
          <span class="pf mono">{{ { 1: 'COZE', 2: 'DIFY', 3: 'OPENAI', 4: 'OTHER' }[cfg.agentPlatform] }}</span>
          <el-radio-group v-model="cfg.unlockRule" size="small" :disabled="!cfg.on">
            <el-radio :value="1">完成全部资料</el-radio>
            <el-radio :value="2">进度阈值</el-radio>
          </el-radio-group>
          <el-input-number v-if="cfg.unlockRule === 2" v-model="cfg.unlockThreshold" :min="1" :max="100" size="small" :disabled="!cfg.on" />
          <span v-if="cfg.unlockRule === 2" class="mono pct">%</span>
        </div>
        <el-button class="mono" type="warning" @click="saveConfig">保存权限关联</el-button>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api/modules'

const route = useRoute()
const projectId = route.params.id
const tab = ref('materials')
const stats = ref({})
const materials = ref([])
const members = ref([])
const users = ref([])
const config = ref([])
const pickUser = ref([])
const mat = reactive({ materialName: '', materialType: 1, materialUrl: '' })

async function load() {
  stats.value = await adminApi.projectStats(projectId)
  materials.value = await adminApi.materialsOf(projectId)
  members.value = await adminApi.members(projectId)
}

async function addMaterial() {
  if (!mat.materialName.trim() || !mat.materialUrl.trim()) {
    ElMessage.warning('请填写资料名称与地址')
    return
  }
  await adminApi.addMaterial(projectId, { ...mat })
  Object.assign(mat, { materialName: '', materialUrl: '' })
  ElMessage.success('已添加，进度已重算')
  await load()
}

async function move(index, dir) {
  const ids = materials.value.map(m => m.materialId)
  const target = index + dir
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  await adminApi.reorderMaterials(ids)
  await load()
}

async function delMaterial(row) {
  await ElMessageBox.confirm('删除后该资料的学习进度将作废并重算成员进度，确认？')
  await adminApi.deleteMaterial(row.materialId)
  await load()
}

async function assign() {
  if (!pickUser.value.length) return
  await adminApi.assignMembers(projectId, pickUser.value)
  ElMessage.success('已分配')
  pickUser.value = []
  await load()
}

async function removeMember(row) {
  await adminApi.removeMember(row.memberId)
  await load()
}

async function loadConfig() {
  const rows = await adminApi.agentConfig(projectId)
  // 与本部门全部智能体并集展示（勾选状态 = 已关联）
  config.value = rows.map(r => ({
    on: true,
    agentId: r.agentId,
    agentName: r.agentName,
    agentPlatform: r.agentPlatform,
    unlockRule: r.unlockRule,
    unlockThreshold: r.unlockThreshold
  }))
}

async function saveConfig() {
  const items = config.value.filter(c => c.on)
    .map(c => ({ agentId: c.agentId, unlockRule: c.unlockRule, unlockThreshold: c.unlockRule === 2 ? c.unlockThreshold : null }))
  await adminApi.saveAgentConfig(projectId, items)
  ElMessage.success('权限关联已保存，实时生效')
}

onMounted(() => {
  load()
  loadConfig()
})
</script>

<style scoped>
@import '../../styles/tokens.css';

.pagehead { display: flex; align-items: flex-end; gap: 24px; padding: 8px 0 16px; border-bottom: 3px solid var(--line-strong); }
h1 { font-size: 34px; font-weight: 900; letter-spacing: -.02em; line-height: 1; }
.meta { margin-left: auto; font-size: 11px; color: var(--text-3); }

.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin: 20px 0; }
.stat { background: var(--surface); border: 1px solid var(--line); border-radius: var(--r); padding: 14px 18px; position: relative; }
.stat::before { content: ""; position: absolute; top: 0; left: 0; width: 32px; height: 3px; background: var(--text); }
.stat.hot::before { background: var(--signal); }
.stat .k { font-size: 11px; color: var(--text-3); }
.stat .v { font-size: 28px; font-weight: 700; }
.stat .v small { font-size: 13px; color: var(--text-3); }
.stat.hot .v em { font-style: normal; color: var(--signal); }

.row-ops { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; flex-wrap: wrap; }
.hint { font-size: 11px; color: var(--text-3); }
.tbl { background: var(--surface); }
.bar { height: 4px; background: var(--surface-2); position: relative; overflow: hidden; }
.bar i { position: absolute; inset: 0; width: var(--w); background: var(--accent); }
.bdg { font-size: 11px; padding: 2px 8px; border: 1px solid var(--line); }
.bdg.unlock { background: var(--signal); border-color: var(--signal); color: #fff; font-weight: 700; }
.bdg.lock { color: var(--text-3); }
.hint-block { background: var(--surface-2); border-left: 3px solid var(--accent); padding: 10px 14px; font-size: 12px; color: var(--text-2); margin-bottom: 14px; }
.cfg { display: flex; align-items: center; gap: 14px; padding: 10px 0; border-bottom: 1px solid var(--line); flex-wrap: wrap; }
.pf { font-size: 10px; border: 1px solid var(--line); padding: 1px 6px; color: var(--text-3); }
.pct { color: var(--text-3); }
</style>
