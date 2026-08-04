<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')
const showPwd = ref(false)
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  error.value = ''
  if (!username.value || !password.value) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    const res = await login({ username: username.value, password: password.value })
    localStorage.setItem('token', res.token)
    localStorage.setItem('user', JSON.stringify({
      userId: res.userId,
      username: res.username,
      nickname: res.nickname,
      email: res.email
    }))
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || '用户名或密码错误'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="login-logo">HomeRecipe</h1>
      <p class="login-sub">AI-powered recipe management</p>

      <div class="form-group">
        <label class="form-label">用户名</label>
        <input
          v-model="username"
          type="text"
          class="form-input"
          placeholder="请输入用户名"
          autocomplete="username"
          @keyup.enter="handleLogin"
        />
      </div>

      <div class="form-group">
        <label class="form-label">密码</label>
        <div class="pwd-wrap">
          <input
            v-model="password"
            :type="showPwd ? 'text' : 'password'"
            class="form-input"
            placeholder="请输入密码"
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          />
          <button class="pwd-toggle" type="button" @click="showPwd = !showPwd">
            {{ showPwd ? '隐藏' : '显示' }}
          </button>
        </div>
      </div>

      <p v-if="error" class="login-error">{{ error }}</p>

      <button class="btn btn-primary btn-block login-btn" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <p class="login-link">
        没有账号？<router-link to="/register">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--bg);
}

.login-card {
  width: 100%;
  max-width: 360px;
}

.login-logo {
  font-size: 32px;
  font-weight: 700;
  text-align: center;
  color: var(--text-primary);
}

.login-sub {
  font-size: 14px;
  color: var(--text-secondary);
  text-align: center;
  margin-top: 4px;
  margin-bottom: 32px;
}

.pwd-wrap {
  position: relative;
}

.pwd-toggle {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  font-size: 13px;
  color: var(--accent);
  padding: 4px 8px;
}

.login-error {
  font-size: 13px;
  color: var(--danger);
  margin-bottom: 12px;
  text-align: center;
}

.login-btn {
  margin-top: 8px;
  height: 44px;
  font-size: 16px;
}

.login-btn:disabled {
  opacity: 0.6;
}

.login-link {
  font-size: 14px;
  color: var(--text-secondary);
  text-align: center;
  margin-top: 20px;
}

.login-link a {
  color: var(--accent);
}
</style>
