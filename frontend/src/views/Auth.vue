<template>
  <div class="auth-container">
    <div class="auth-box">
      <div class="auth-header">
        <h1>{{ isLogin ? '登录' : '注册' }}</h1>
      </div>
      
      <form @submit.prevent="handleSubmit" class="auth-form">
        <div v-if="!isLogin" class="form-group">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model="form.username"
            type="text"
            placeholder="请输入用户名"
            required
          />
        </div>
        
        <div v-if="isLogin" class="form-group">
          <label for="uid">UID</label>
          <input
            id="uid"
            v-model="form.uid"
            type="text"
            placeholder="请输入UID"
            required
          />
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            required
          />
        </div>
        
        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '处理中...' : (isLogin ? '登录' : '注册') }}
        </button>
      </form>
      
      <div class="auth-footer">
        <span>{{ isLogin ? '还没有账号？' : '已有账号？' }}</span>
        <a @click="toggleMode">{{ isLogin ? '立即注册' : '立即登录' }}</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { login, register } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const isLogin = ref(true)
const loading = ref(false)
const form = ref({
  username: '',
  uid: '',
  password: ''
})

function toggleMode() {
  isLogin.value = !isLogin.value
  form.value = {
    username: '',
    uid: '',
    password: ''
  }
}

async function handleSubmit() {
  if (loading.value) return
  
  loading.value = true
  
  try {
    let res
    if (isLogin.value) {
      res = await login({
        uid: form.value.uid,
        password: form.value.password
      })
    } else {
      res = await register({
        username: form.value.username,
        password: form.value.password
      })
    }
    
    if (res.code === 200) {
      if (isLogin.value) {
        appStore.showToast('登录成功')
        userStore.setUser(res.data)
        router.push('/posts')
      } else {
        // 注册成功后显示UID
        const uid = res.data.uid
        appStore.showToast(`注册成功！您的UID是：${uid}，请牢记`)
        userStore.setUser(res.data)
        // 延迟跳转，让用户有时间记住UID
        setTimeout(() => {
          router.push('/posts')
        }, 3000)
      }
    }
  } catch (error) {
    appStore.showToast(error.message || '操作失败', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.auth-box {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  padding: 40px;
  width: 100%;
  max-width: 400px;
}

.auth-header h1 {
  font-size: 28px;
  color: #333;
  margin-bottom: 30px;
  text-align: center;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.form-group input {
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.submit-btn {
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.auth-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  color: #666;
}

.auth-footer a {
  color: #667eea;
  cursor: pointer;
  font-weight: 500;
}

.auth-footer a:hover {
  text-decoration: underline;
}
</style>
