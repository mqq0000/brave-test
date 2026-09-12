import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin, register as apiRegister, logout as apiLogout, getMyProfile } from '../api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const role = ref(localStorage.getItem('role') || '')
  const profile = ref(null)

  const isAdmin = () =>
    ['SUPER_ADMIN', 'XINGZHE_ADMIN', 'ADVENTURER_ADMIN', 'KNOWLEDGE_ADMIN'].includes(role.value)

  async function login(form) {
    const data = await apiLogin(form)
    token.value = data.token
    role.value = data.role
    localStorage.setItem('token', data.token)
    localStorage.setItem('role', data.role)
    return data
  }

  async function register(form) {
    return apiRegister(form)
  }

  async function logout() {
    try {
      await apiLogout()
    } catch (e) {
      /* 忽略登出接口异常 */
    }
    token.value = ''
    role.value = ''
    profile.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('profile')
  }

  async function loadProfile() {
    profile.value = await getMyProfile()
    localStorage.setItem('profile', JSON.stringify(profile.value))
    return profile.value
  }

  return { token, role, profile, isAdmin, login, register, logout, loadProfile }
})
