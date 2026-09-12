<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <div class="title">
          <span class="sprout"></span> 勇者测试
          <div class="subtitle">高污染世界 · 守住你的勇者萌芽</div>
        </div>
      </template>
      <el-tabs v-model="tab">
        <el-tab-pane label="登录" name="login">
          <div class="prologue">
            世界被污染侵蚀，人们的勇者萌芽正在变灰、变红。<br />
            而你，听见了萌芽的低语——<br />
            学会辨别负能量，守住内心的火种，相信自身的力量。<br />
            众人同心，方能共建美好的世界。
          </div>
          <el-form :model="loginForm" label-width="0">
            <el-form-item>
              <el-input v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="loginForm.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" @keyup.enter="doLogin" />
            </el-form-item>
            <el-button type="primary" style="width: 100%" :loading="loading" @click="doLogin">进入世界</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="成为冒险者" name="register">
          <el-form :model="regForm" label-width="0">
            <el-form-item>
              <el-input v-model="regForm.username" placeholder="用户名（3~32位）" :prefix-icon="User" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="regForm.nickname" placeholder="冒险者名号" :prefix-icon="EditPen" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="regForm.password" type="password" placeholder="密码（6~32位）" show-password :prefix-icon="Lock" />
            </el-form-item>
            <el-button type="success" style="width: 100%" :loading="loading" @click="doRegister">觉醒勇者萌芽</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, EditPen } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const tab = ref('login')
const loading = ref(false)
const loginForm = ref({ username: '', password: '' })
const regForm = ref({ username: '', nickname: '', password: '' })

async function doLogin() {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(loginForm.value)
    ElMessage.success('欢迎回到这个高污染的世界')
    router.push('/tasks')
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  loading.value = true
  try {
    await auth.register(regForm.value)
    ElMessage.success('觉醒成功！你的冒险者萌芽已苏醒，D级冒险者，出发吧')
    tab.value = 'login'
    loginForm.value.username = regForm.value.username
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #eef2f7 0%, #dfe7f1 100%);
}
.login-card {
  width: 400px;
}
.title {
  font-size: 20px;
  font-weight: 500;
  text-align: center;
}
.subtitle {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  font-weight: 400;
}
.sprout {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50% 50% 50% 0;
  background: #67c23a;
  margin-right: 6px;
}
.prologue {
  font-size: 12px;
  color: #909399;
  line-height: 1.9;
  margin-bottom: 16px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
}
</style>
