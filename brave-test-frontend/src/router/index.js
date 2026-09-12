import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/tasks',
    children: [
      { path: 'story', name: 'story', component: () => import('../views/Story.vue'), meta: { title: '冒险剧情' } },
      { path: 'test', name: 'test', component: () => import('../views/Test.vue'), meta: { title: '觉醒测试' } },
      { path: 'tasks', name: 'tasks', component: () => import('../views/TaskHall.vue'), meta: { title: '任务大厅' } },
      { path: 'shop', name: 'shop', component: () => import('../views/Shop.vue'), meta: { title: '冒险者商城' } },
      { path: 'knowledge', name: 'knowledge', component: () => import('../views/Knowledge.vue'), meta: { title: '知识宝库' } },
      { path: 'profile', name: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '我的档案' } },
      { path: 'admin', name: 'admin', component: () => import('../views/Admin.vue'), meta: { title: '管理后台' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.name !== 'login' && !localStorage.getItem('token')) {
    return { name: 'login' }
  }
  if (to.name === 'admin') {
    const role = localStorage.getItem('role') || ''
    const admins = ['SUPER_ADMIN', 'XINGZHE_ADMIN', 'ADVENTURER_ADMIN', 'KNOWLEDGE_ADMIN']
    if (!admins.includes(role)) {
      return { name: 'tasks' }
    }
  }
  return true
})

export default router
