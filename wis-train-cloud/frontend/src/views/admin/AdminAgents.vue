<template>
  <div>
    <div class="pagehead">
      <h1>智能体管理</h1>
      <div class="ops">
        <el-input v-model="keyword" class="kw" placeholder="搜索智能体" clearable @change="load" />
        <el-button class="mono" type="warning" @click="openCreate">+ 录入智能体</el-button>
      </div>
    </div>

    <el-table :data="rows" class="tbl" v-loading="loading">
      <el-table-column prop="agentId" label="ID" width="64" class-name="mono" />
      <el-table-column prop="agentName" label="名称" min-width="150" />
      <el-table-column label="平台" width="90">
        <template #default="{ row }"><span class="pf mono">{{ { 1: 'COZE', 2: 'DIFY', 3: 'OPENAI', 4: 'OTHER' }[row.agentPlatform] }}</span></template>
      </el-table-column>
      <el-table-column prop="agentEndpoint" label="接口地址" min-width="220" class-name="mono" show-overflow-tooltip>
        <template #default="{ row }">{{ row.agentEndpoint || '（未配置 → Mock 演示）' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <span class="bdg mono" :class="row.agentState === 1 ? 'ok' : 'off'">{{ row.agentState === 1 ? '启用' : '停用' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170">
        <template #default="{ row }">
          <el-button size="small" text type="primary" class="mono" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text :type="row.agentState === 1 ? 'danger' : 'success'" class="mono" @click="toggle(row)">
            {{ row.agentState === 1 ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" layout="total, prev, pager, next" :total="total" :page-size="size" v-model:current-page="pageNum" @current-change="load" />

    <!-- 录入 / 编辑（US-401）：Key 仅落库不回显 -->
    <el-dialog v-model="dlg" :title="editing ? '编辑智能体' : '录入智能体（生成独立会话入口）'" width="560">
      <el-form label-width="92">
        <el-form-item label="名称"><el-input v-model="form.agentName" /></el-form-item>
        <el-form-item label="平台类型">
          <el-select v-model="form.agentPlatform" style="width: 200px">
            <el-option :value="1" label="Coze" /><el-option :value="2" label="Dify" />
            <el-option :value="3" label="OpenAI 兼容" /><el-option :value="4" label="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.agentApiKey" :placeholder="editing ? '回显掩码，不改动则留掩码' : '第三方平台密钥（AES 加密存储）'" show-password />
        </el-form-item>
        <el-form-item label="接口地址">
          <el-input v-model="form.agentEndpoint" class="mono" placeholder="留空则使用 Mock 流式演示" />
        </el-form-item>
        <el-form-item label="模型标识"><el-input v-model="form.agentModel" class="mono" placeholder="如 gpt-4o-mini / 自定义" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.agentDesc" type="textarea" :rows="2" placeholder="智能体用途与适用场景（员工会话页可见）" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button class="mono" @click="dlg = false">取消</el-button>
        <el-button class="mono" type="warning" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api/modules'

const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const size = 10
const keyword = ref('')
const loading = ref(false)
const dlg = ref(false)
const editing = ref(false)
const form = reactive({ agentId: null, agentName: '', agentPlatform: 1, agentApiKey: '', agentEndpoint: '', agentModel: '', agentDesc: '' })

async function load() {
  loading.value = true
  try {
    const page = await adminApi.agentPage({ pageNum: pageNum.value, pageSize: size, keyword: keyword.value })
    rows.value = page.items
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  Object.assign(form, { agentId: null, agentName: '', agentPlatform: 1, agentApiKey: '', agentEndpoint: '', agentModel: '', agentDesc: '' })
  dlg.value = true
}

function openEdit(row) {
  editing.value = true
  Object.assign(form, { ...row, agentApiKey: '****' + (row.agentId % 10000) })
  dlg.value = true
}

async function save() {
  if (!form.agentName.trim() || !form.agentApiKey.trim()) {
    ElMessage.warning('请填写名称与 API Key')
    return
  }
  if (editing.value) await adminApi.updateAgent({ ...form })
  else await adminApi.createAgent({ ...form })
  ElMessage.success('已保存，会话入口已生成')
  dlg.value = false
  await load()
}

async function toggle(row) {
  await adminApi.updateAgentState(row.agentId, row.agentState === 1 ? 0 : 1)
  ElMessage.success(row.agentState === 1 ? '已停用，关联会话入口关闭' : '已启用')
  await load()
}

onMounted(load)
</script>

<style scoped>
@import '../../styles/tokens.css';

.pagehead { display: flex; align-items: flex-end; gap: 24px; padding: 8px 0 16px; border-bottom: 3px solid var(--line-strong); }
h1 { font-size: 34px; font-weight: 900; letter-spacing: -.02em; line-height: 1; }
.ops { margin-left: auto; display: flex; gap: 10px; }
.kw { width: 220px; }
.tbl { margin-top: 20px; }
.pf { font-size: 10px; border: 1px solid var(--line); padding: 1px 6px; color: var(--text-3); }
.bdg { font-size: 11px; padding: 2px 8px; border: 1px solid var(--line); }
.bdg.ok { color: var(--ok); border-color: currentColor; }
.bdg.off { color: var(--danger); border-color: currentColor; }
.pager { margin-top: 16px; }
</style>
