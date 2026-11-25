<template>
  <el-container class="shell grid-atmos">
    <!-- 深色侧栏：工业风 -->
    <el-aside width="208px" class="side">
      <div class="logo mono"><span class="sig"></span>智培云 <b>WIS</b>_CLOUD</div>
      <el-menu :default-active="route.path" router class="side-menu" background-color="transparent">
        <template v-if="auth.role === 1">
          <el-menu-item index="/admin"><span class="mi mono">■</span>工作台</el-menu-item>
          <el-menu-item index="/admin/projects"><span class="mi mono">■</span>项目管理</el-menu-item>
          <el-menu-item index="/admin/agents"><span class="mi mono">■</span>智能体管理</el-menu-item>
          <el-menu-item index="/admin/users"><span class="mi mono">■</span>员工管理</el-menu-item>
        </template>
        <template v-else>
          <el-menu-item index="/workspace"><span class="mi mono">■</span>我的项目</el-menu-item>
          <el-menu-item index="/workspace/agents"><span class="mi mono">■</span>我的智能体</el-menu-item>
        </template>
      </el-menu>
      <div class="side-foot mono">WISTRAIN_V1.0</div>
    </el-aside>

    <el-container class="body">
      <!-- 顶栏 -->
      <header class="topbar">
        <span class="crumb mono">{{ crumb }}</span>
        <span class="spacer"></span>
        <span class="user mono">{{ auth.realname }}<i class="lamp"></i></span>
        <el-button text @click="onLogout">退出</el-button>
      </header>
      <main class="content">
        <router-view />
      </main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { authApi } from '../api/modules'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const crumb = computed(() => route.path.replace(/^\//, '').replace(/\//g, ' / ').toUpperCase() || 'INDEX')

async function onLogout() {
  try { await authApi.logout() } catch { /* 忽略 */ }
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.shell { height: 100vh; }
.side {
  background:
    linear-gradient(rgba(247,247,245,.05) 1px, transparent 1px) 0 0/40px 40px,
    var(--dark);
  color: #F5F1EA;
  display: flex; flex-direction: column;
}
.logo { padding: 20px 18px; font-size: 14px; border-bottom: 1px solid rgba(245,241,234,.12); }
.logo b { color: #fff; }
.sig { display: inline-block; width: 8px; height: 8px; background: var(--signal); margin-right: 8px; }
.side-menu { border-right: none; flex: 1; }
.side-menu :deep(.el-menu-item) {
  color: #A09A90;
  font-size: 13px;
  height: 44px;
  margin: 2px 8px;
  border-radius: var(--r);
}
.side-menu :deep(.el-menu-item:hover) { background: var(--dark-2); color: #F5F1EA; }
.side-menu :deep(.el-menu-item.is-active) { background: var(--signal); color: #fff; font-weight: 700; }
.mi { margin-right: 8px; font-size: 10px; }
.side-foot { padding: 14px 18px; font-size: 10px; color: #6B6660; letter-spacing: .1em; }

.body { flex-direction: column; }
.topbar {
  height: 52px;
  display: flex; align-items: center; gap: 16px;
  padding: 0 24px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}
.crumb { font-size: 11px; color: var(--text-3); letter-spacing: .1em; }
.spacer { flex: 1; }
.user { font-size: 13px; color: var(--text-2); display: inline-flex; align-items: center; gap: 8px; }
.lamp { width: 7px; height: 7px; border-radius: 50%; background: var(--ok); display: inline-block; }
.content { flex: 1; padding: 24px 32px; overflow: auto; z-index: 1; }
</style>
