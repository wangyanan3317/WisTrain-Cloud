import { defineStore } from 'pinia'

/** 登录态：token + 用户信息（localStorage 持久化） */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('wt_token') || '',
    userId: localStorage.getItem('wt_userId') || '',
    realname: localStorage.getItem('wt_realname') || '',
    role: localStorage.getItem('wt_role') || '',
    deptId: localStorage.getItem('wt_deptId') || ''
  }),
  actions: {
    setLogin(resp) {
      this.token = resp.token
      this.userId = resp.userId
      this.realname = resp.realname
      this.role = resp.role
      this.deptId = resp.deptId
      localStorage.setItem('wt_token', resp.token)
      localStorage.setItem('wt_userId', resp.userId)
      localStorage.setItem('wt_realname', resp.realname)
      localStorage.setItem('wt_role', resp.role)
      localStorage.setItem('wt_deptId', resp.deptId)
    },
    /** 安全退出（US-102）：清除令牌 → 登录页 */
    logout() {
      this.$reset()
      ;['wt_token', 'wt_userId', 'wt_realname', 'wt_role', 'wt_deptId'].forEach(k => localStorage.removeItem(k))
    }
  }
})
