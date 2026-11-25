<template>
  <div>
    <div class="pagehead">
      <h1>项目管理</h1>
      <div class="ops">
        <el-input v-model="keyword" class="mono kw" placeholder="搜索项目名" clearable @change="load" />
        <el-button class="mono" type="warning" @click="openCreate">+ 新建项目</el-button>
      </div>
    </div>

    <el-table :data="rows" class="tbl" v-loading="loading">
      <el-table-column prop="projectId" label="ID" width="64" class-name="mono" />
      <el-table-column prop="projectName" label="项目名称" min-width="180" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <span class="bdg mono" :class="{ ok: row.projectState === 1, end: row.projectState === 2 }">
            {{ ['', '进行中', '已结束'][row.projectState] || '草稿' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="起止" width="200" class-name="mono">
        <template #default="{ row }">{{ fmt(row.projectStart) }} ~ {{ fmt(row.projectEnd) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230">
        <template #default="{ row }">
          <el-button size="small" text type="primary" class="mono" @click="$router.push('/admin/projects/' + row.projectId)">详情</el-button>
          <el-button v-if="row.projectState === 0" size="small" text type="warning" class="mono" @click="publish(row)">发布</el-button>
          <el-button v-if="row.projectState !== 2" size="small" text class="mono" @click="archive(row)">归档</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" layout="total, prev, pager, next" :total="total"
      :page-size="size" v-model:current-page="pageNum" @current-change="load" />

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="dlg" :title="editing ? '编辑项目' : '新建项目（草稿）'" width="520">
      <el-form label-width="84">
        <el-form-item label="项目名称"><el-input v-model="form.projectName" /></el-form-item>
        <el-form-item label="项目描述"><el-input v-model="form.projectDesc" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="起止时间">
          <el-date-picker v-model="range" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api/modules'

const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const size = 10
const keyword = ref('')
const loading = ref(false)
const dlg = ref(false)
const editing = ref(false)
const range = ref([])
const form = reactive({ projectId: null, projectName: '', projectDesc: '' })

const fmt = t => t ? String(t).replace('T', ' ').slice(0, 16) : '—'

async function load() {
  loading.value = true
  try {
    const page = await adminApi.projectPage({ pageNum: pageNum.value, pageSize: size, keyword: keyword.value })
    rows.value = page.items
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  Object.assign(form, { projectId: null, projectName: '', projectDesc: '' })
  range.value = []
  dlg.value = true
}

async function save() {
  if (!form.projectName.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  form.projectStart = range.value?.[0] || null
  form.projectEnd = range.value?.[1] || null
  if (editing.value) await adminApi.updateProject({ ...form })
  else await adminApi.createProject({ ...form })
  ElMessage.success('已保存')
  dlg.value = false
  await load()
}

async function publish(row) {
  await adminApi.publishProject(row.projectId)
  ElMessage.success('已发布，员工端可见')
  await load()
}

async function archive(row) {
  await ElMessageBox.confirm('归档后员工端不再展示，关联智能体访问将关闭（Q5）。确认归档？')
  await adminApi.archiveProject(row.projectId)
  ElMessage.success('已归档')
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
.bdg { font-size: 11px; padding: 2px 8px; border: 1px solid var(--line); color: var(--text-2); }
.bdg.ok { color: var(--ok); border-color: currentColor; }
.bdg.end { color: var(--text-3); }
.pager { margin-top: 16px; }
</style>
