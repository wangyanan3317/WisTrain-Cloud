import request, { chatStream } from './request'

/** 全部 API（按设计说明书 4.2 接口清单） */
export const authApi = {
  login: data => request.post('/auth/login', data),
  logout: () => request.post('/auth/logout')
}

export const projectApi = {
  my: () => request.get('/projects/my'),
  available: () => request.get('/projects/available'),
  join: id => request.post(`/projects/${id}/join`),
  materials: id => request.get(`/projects/${id}/materials`),
  finish: materialId => request.post(`/projects/materials/${materialId}/finish`)
}

export const agentApi = {
  my: () => request.get('/agents/my'),
  sessions: id => request.get(`/agents/${id}/sessions`),
  chatStream: chatStream // SSE 见 request.js
}

export const adminApi = {
  // 项目
  projectPage: params => request.get('/admin/projects', { params }),
  createProject: data => request.post('/admin/projects', data),
  updateProject: data => request.put('/admin/projects', data),
  publishProject: id => request.put(`/admin/projects/${id}/publish`),
  archiveProject: id => request.put(`/admin/projects/${id}/archive`),
  projectStats: id => request.get(`/admin/projects/${id}/stats`),
  materialsOf: id => request.get(`/admin/projects/${id}/materials`),
  addMaterial: (id, data) => request.post(`/admin/projects/${id}/materials`, data),
  updateMaterial: data => request.put('/admin/projects/materials', data),
  deleteMaterial: id => request.delete(`/admin/projects/materials/${id}`),
  reorderMaterials: ids => request.put('/admin/projects/materials/order', ids),
  members: id => request.get(`/admin/projects/${id}/members`),
  assignMembers: (id, userIds) => request.post(`/admin/projects/${id}/members`, { userIds }),
  removeMember: memberId => request.delete(`/admin/projects/members/${memberId}`),
  // 智能体
  agentPage: params => request.get('/admin/agents', { params }),
  createAgent: data => request.post('/admin/agents', data),
  updateAgent: data => request.put('/admin/agents', data),
  updateAgentState: (id, state) => request.put(`/admin/agents/${id}/state`, { state }),
  agentConfig: projectId => request.get(`/admin/projects/${projectId}/agents`),
  saveAgentConfig: (projectId, items) => request.put(`/admin/projects/${projectId}/agents`, items),
  // 员工
  userPage: params => request.get('/admin/users', { params }),
  createUser: data => request.post('/admin/users', data),
  updateUserState: (id, state) => request.put(`/admin/users/${id}/state`, { state }),
  resetPwd: id => request.put(`/admin/users/${id}/password`),
  // 看板
  statsSummary: () => request.get('/admin/stats/summary'),
  statsTrend: () => request.get('/admin/stats/trend')
}
