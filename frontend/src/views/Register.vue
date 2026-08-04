<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')
const nickname = ref('')
const email = ref('')
const showPwd = ref(false)
const loading = ref(false)
const error = ref('')

async function handleRegister() {
  error.value = ''
  if (!username.value || !password.value) {
    error.value = '请输入用户名和密码'
    return
  }
  if (password.value.length < 6) {
    error.value = '密码至少需要6位'
    return
  }
  loading.value = true
  try {
    await register({
      username: username.value,
      password: password.value,
      nickname: nickname.value || undefined,
      email: email.value || undefined
    })
    router.push('/login')
  } catch (e) {
    error.value = e.response?.data?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-card">
      <h1 class="register-logo">HomeRecipe</h1>
      <p class="register-sub">创建你的专属食谱账户</p>

      <div class="form-group">
        <label class="form-label">用户名</label>
        <input
          v-model="username"
          type="text"
          class="form-input"
          placeholder="登录时使用的用户名"
          autocomplete="username"
          @keyup.enter="handleRegister"
        />
      </div>

      <div class="form-group">
        <label class="form-label">昵称 <span class="optional">(选填)</span></label>
        <input
          v-model="nickname"
          type="text"
          class="form-input"
          placeholder="如何称呼你"
          @keyup.enter="handleRegister"
        />
      </div>

      <div class="form-group">
        <label class="form-label">邮箱 <span class="optional">(选填)</span></label>
        <input
          v-model="email"
          type="email"
          class="form-input"
          placeholder="用于找回密码"
          autocomplete="email"
          @keyup.enter="handleRegister"
        />
      </div>

      <div class="form-group">
        <label class="form-label">密码</label>
        <div class="pwd-wrap">
          <input
            v-model="password"
            :type="showPwd ? 'text' : 'password'"
            class="form-input"
            placeholder="至少6位密码"
            autocomplete="new-password"
            @keyup.enter="handleRegister"
          />
          <button class="pwd-toggle" type="button" @click="showPwd = !showPwd">
            {{ showPwd ? '隐藏' : '显示' }}
          </button>
        </div>
      </div>

      <p v-if="error" class="register-error">{{ error }}</p>

      <button class="btn btn-primary btn-block register-btn" :disabled="loading" @click="handleRegister">
        {{ loading ? '注册中...' : '注册' }}
      </button>

      <p class="register-link">
        已有账号？<router-link to="/login">立即登录</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--bg);
}

.register-card {
  width: 100%;
  max-width: 360px;
}

.register-logo {
  font-size: 32px;
  font-weight: 700;
  text-align: center;
  color: var(--text-primary);
}

.register-sub {
  font-size: 14px;
  color: var(--text-secondary);
  text-align: center;
  margin-top: 4px;
  margin-bottom: 32px;
}

.optional {
  font-weight: 400;
  color: var(--text-secondary);
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

.register-error {
  font-size: 13px;
  color: var(--danger);
  margin-bottom: 12px;
  text-align: center;
}

.register-btn {
  margin-top: 8px;
  height: 44px;
  font-size: 16px;
}

.register-btn:disabled {
  opacity: 0.6;
}

.register-link {
  font-size: 14px;
  color: var(--text-secondary);
  text-align: center;
  margin-top: 20px;
}

.register-link a {
  color: var(--accent);
}
</style>
