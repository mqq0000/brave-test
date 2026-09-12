<template>
  <el-container style="min-height: 100vh">
    <el-aside width="200px" class="aside">
      <div class="brand"><span class="sprout"></span> 勇者测试</div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/story"><el-icon><Guide /></el-icon>冒险剧情</el-menu-item>
        <el-menu-item index="/test"><el-icon><MagicStick /></el-icon>觉醒测试</el-menu-item>
        <el-menu-item index="/tasks"><el-icon><List /></el-icon>任务大厅</el-menu-item>
        <el-menu-item index="/shop"><el-icon><ShoppingCart /></el-icon>冒险者商城</el-menu-item>
        <el-menu-item index="/knowledge"><el-icon><Reading /></el-icon>知识宝库</el-menu-item>
        <el-menu-item index="/profile"><el-icon><User /></el-icon>我的档案</el-menu-item>
        <el-menu-item v-if="auth.isAdmin()" index="/admin"><el-icon><Setting /></el-icon>管理后台</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="page-title">{{ $route.meta.title }}</span>
        <el-dropdown>
          <span class="user-chip">
            <el-icon><Avatar /></el-icon>
            {{ auth.profile?.nickname || '冒险者' }}
            <el-tag v-if="auth.profile" size="small" style="margin-left: 6px">{{ auth.profile.rankLevel }} 级</el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/profile')">我的档案</el-dropdown-item>
              <el-dropdown-item divided @click="doLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const router = useRouter()

onMounted(() => {
  if (!auth.profile) auth.loadProfile().catch(() => {})
})

async function doLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.aside {
  background: #fff;
  border-right: 1px solid #ebeef5;
}
.brand {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  font-weight: 500;
  border-bottom: 1px solid #ebeef5;
}
.sprout {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50% 50% 50% 0;
  background: #67c23a;
  margin-right: 8px;
}
.header {
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-title {
  font-size: 16px;
  font-weight: 500;
}
.user-chip {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #303133;
}
</style>
