import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

// 路由设计（设计说明书 6.2）：员工端 /workspace，管理端 /admin
const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
  {
    path: '/',
    component: () => import('../layouts/ShellLayout.vue'),
    redirect: '/workspace',
    children: [
      // 员工端
      { path: 'workspace', name: 'workspace', component: () => import('../views/employee/MyProjects.vue') },
      { path: 'workspace/learn/:projectId', name: 'learn', component: () => import('../views/employee/LearnCenter.vue') },
      { path: 'workspace/agents', name: 'agents', component: () => import('../views/employee/MyAgents.vue') },
      { path: 'workspace/agents/:agentId/chat', name: 'chat', component: () => import('../views/employee/AgentChat.vue') },
      // 管理端（auth 管理员角色）
      { path: 'admin', name: 'admin', component: () => import('../views/admin/AdminDashboard.vue') },
      { path: 'admin/projects', name: 'adminProjects', component: () => import('../views/admin/AdminProjects.vue') },
      { path: 'admin/projects/:id', name: 'adminProjectDetail', component: () => import('../views/admin/AdminProjectDetail.vue') },
      { path: 'admin/agents', name: 'adminAgents', component: () => import('../views/admin/AdminAgents.vue') },
      { path: 'admin/users', name: 'adminUsers', component: () => import('../views/admin/AdminUsers.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/workspace' }
]

const router = createRouter({ history: createWebHistory(), routes })

// 路由守卫：未登录 → /login；员工访问管理端 → 回工作台
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path !== '/login' && !auth.token) return '/login'
  if (to.path.startsWith('/admin') && auth.role !== 1) return '/workspace'
})

export default router
