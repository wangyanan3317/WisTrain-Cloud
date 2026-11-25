<template>
  <div>
    <div class="pagehead">
      <h1>员工管理</h1>
      <div class="ops">
        <el-input v-model="keyword" class="kw" placeholder="搜索姓名 / 登录名" clearable @change="load" />
        <el-button class="mono" type="warning" @click="openCreate">+ 新增员工</el-button>
      </div>
    </div>

    <el-table :data="rows" class="tbl" v-loading="loading">
      <el-table-column prop="userId" label="ID" width="64" class-name="mono" />
      <el-table-column prop="realname" label="姓名" width="120" />
      <el-table-column prop="logname" label="登录名" min-width="150" class-name="mono" />
      <el-table-column prop="email" label="邮箱" min-width="180" class-name="mono" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <span class="bdg mono" :class="row.state === 1 ? 'ok' : 'off'">{{ row.state === 1 ? '正常' : '禁用' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210">
        <template #default="{ row }">
          <el-button size="small" text :type="row.state === 1 ? 'danger' : 'success'" class="mono" @click="toggle(row)">
            {{ row.state === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" text type="primary" class="mono" @click="resetPwd(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" layout="total, prev, pager, next" :total="total" :page-size="size" v-model:current-page="pageNum" @current-change="load" />

    <el-dialog v-model="dlg" title="新增员工（US-501）" width="480">
      <el-form label-width="92">
        <el-form-item label="登录名"><el-input v-model="form.logname" class="mono" placeholder="工号 / 邮箱" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realname" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" class="mono" /></el-form-item>
        <el-form-item label="初始密码">
          <el-input v-model="form.pwd" class="mono" placeholder="留空则自动生成 8 位随机密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="mono" @click="dlg = false">取消</el-button>
        <el-button class="mono" type="warning" @click="save">创建</el-button>
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
const form = reactive({ logname: '', realname: '', email: '', pwd: '' })

async function load() {
  loading.value = true
  try {
    const page = await adminApi.userPage({ pageNum: pageNum.value, pageSize: size, keyword: keyword.value })
    rows.value = page.items
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { logname: '', realname: '', email: '', pwd: '' })
  dlg.value = true
}

async function save() {
  if (!form.logname.trim() || !form.realname.trim()) {
    ElMessage.warning('请填写登录名与姓名')
    return
  }
  await adminApi.createUser({ ...form })
  ElMessage.success('已创建')
  dlg.value = false
  await load()
}

async function toggle(row) {
  await adminApi.updateUserState(row.userId, row.state === 1 ? 0 : 1)
  ElMessage.success(row.state === 1 ? '已禁用' : '已启用')
  await load()
}

async function resetPwd(row) {
  const temp = await adminApi.resetPwd(row.userId)
  await ElMessageBox.alert(`临时密码：<b class="mono">${temp}</b>（仅显示一次，请转交员工）`, '重置成功', { dangerouslyUseHTMLString: true })
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
.bdg { font-size: 11px; padding: 2px 8px; border: 1px solid var(--line); }
.bdg.ok { color: var(--ok); border-color: currentColor; }
.bdg.off { color: var(--danger); border-color: currentColor; }
.pager { margin-top: 16px; }
</style>
