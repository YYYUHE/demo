import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { checkAuth, getCurrentUserFullProfile, logout as logoutApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const userProfile = ref(null)
  const isLoggedIn = computed(() => !!user.value)

  async function checkLoginStatus() {
    try {
      const res = await checkAuth()
      if (res.code === 200) {
        user.value = res.data
        return true
      }
      return false
    } catch (error) {
      console.error('检查登录状态失败:', error)
      return false
    }
  }

  async function loadUserProfile() {
    try {
      const res = await getCurrentUserFullProfile()
      if (res.code === 200) {
        userProfile.value = res.data
        return res.data
      }
      return null
    } catch (error) {
      console.error('加载用户资料失败:', error)
      return null
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (error) {
      console.error('登出失败:', error)
    } finally {
      user.value = null
      userProfile.value = null
    }
  }

  function setUser(userData) {
    user.value = userData
  }

  function setUserProfile(profileData) {
    userProfile.value = profileData
  }

  return {
    user,
    userProfile,
    isLoggedIn,
    checkLoginStatus,
    loadUserProfile,
    logout,
    setUser,
    setUserProfile
  }
})
