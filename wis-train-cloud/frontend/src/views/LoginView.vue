<template>
  <div class="login-page">
    <!-- 左：深色品牌区（工业风：网格纹理 + 等宽字 + 唯一强调橙） -->
    <aside class="brand">
      <div class="brand-top mono"><span class="sig"></span>智培云 <b>WISTRAIN</b>_CLOUD</div>
      <h1>先培训<br>后解锁</h1>
      <p class="desc">
        培训资料与配套 AI 助手一体化管理。<br>
        完成培训项目学习，自动解锁对应的智能体——<br>
        资格由服务端实时判定，不靠前端置灰。
      </p>
      <div class="pipeline mono">
        <span class="step">JOIN</span><i></i><span class="step">LEARN</span><i></i><span class="step hot">UNLOCK</span><i></i><span class="step">CHAT</span>
      </div>
      <div class="brand-foot mono">B0304106 · 企业项目实战实训 · V1.0</div>
    </aside>

    <!-- 右：登录表单 -->
    <main class="form-area">
      <el-form class="login-form" @submit.prevent>
        <h2>登录 <span class="mono sub">SYS.AUTH / LOGIN</span></h2>
        <el-form-item>
          <el-input v-model="form.logname" size="large" placeholder="登录名（工号 / 邮箱）" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.pwd" size="large" type="password" show-password placeholder="密码" @keyup.enter="submit" />
        </el-form-item>
        <el-button class="go" size="large" type="warning" :loading="loading" native-type="submit" @click="submit">
          {{ loading ? '验证中…' : '登录 →' }}
        </el-button>
        <p class="tip mono">登录后按角色分流：员工 → 工作台 · 管理员 → 后台</p>
      </el-form>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api/modules'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ logname: '', pwd: '' })

async function submit() {
  if (!form.logname || !form.pwd) {
    ElMessage.warning('请输入登录名和密码')
    return
  }
  loading.value = true
  try {
    const resp = await authApi.login({ logname: form.logname, pwd: form.pwd })
    auth.setLogin(resp)
    // US-101：按角色分流
    router.push(resp.role === 1 ? '/admin' : '/workspace')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 分栏非对称：左 46% 深色品牌区，右表单区 */
.login-page { display: flex; height: 100vh; }

.brand {
  flex: 0 0 46%;
  background:
    linear-gradient(rgba(247,247,245,.06) 1px, transparent 1px) 0 0/44px 44px,
    linear-gradient(90deg, rgba(247,247,245,.06) 1px, transparent 1px) 0 0/44px 44px,
    var(--dark);
  color: #F5F1EA;
  padding: 32px 48px;
  display: flex; flex-direction: column;
  position: relative;
  overflow: hidden;
}
.brand::after {
  content: ""; position: absolute; inset: 0; pointer-events: none;
  background: radial-gradient(40rem 28rem at 110% 0%, rgba(255,107,44,.16), transparent 55%);
}
.brand-top { font-size: 14px; letter-spacing: .02em; }
.brand-top b { color: #fff; }
.sig { display: inline-block; width: 8px; height: 8px; background: var(--signal); margin-right: 8px; }
.brand h1 {
  margin-top: 14vh;
  font-size: clamp(56px, 7vw, 96px);
  font-weight: 900;
  line-height: 1.02;
  letter-spacing: -.03em;
}
.desc { margin-top: 24px; color: #A09A90; font-size: 14px; line-height: 1.9; }
.pipeline { margin-top: auto; display: flex; align-items: center; gap: 10px; font-size: 12px; letter-spacing: .08em; }
.pipeline .step { border: 1px solid rgba(245,241,234,.25); padding: 4px 10px; color: #A09A90; }
.pipeline .step.hot { background: var(--signal); border-color: var(--signal); color: #fff; font-weight: 700; }
.pipeline i { flex: 0 0 18px; height: 1px; background: rgba(245,241,234,.25); }
.brand-foot { margin-top: 16px; font-size: 11px; color: #6B6660; }

.form-area { flex: 1; display: grid; place-items: center; background: var(--bg); }
.login-form { width: min(400px, 86%); }
.login-form h2 { font-size: 26px; font-weight: 800; margin-bottom: 32px; letter-spacing: -.02em; }
.login-form h2 .sub { font-size: 11px; font-weight: 400; color: var(--text-3); margin-left: 10px; letter-spacing: .08em; }
.go { width: 100%; margin-top: 8px; font-weight: 700; letter-spacing: .06em; }
.tip { margin-top: 20px; font-size: 11px; color: var(--text-3); text-align: center; }

@media (max-width: 768px) {
  .brand { display: none; }
}
</style>
