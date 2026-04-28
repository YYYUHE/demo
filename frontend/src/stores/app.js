import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const loading = ref(false)
  const toast = ref({
    show: false,
    message: '',
    type: 'success'
  })

  function setLoading(value) {
    loading.value = value
  }

  function showToast(message, type = 'success') {
    toast.value = {
      show: true,
      message,
      type
    }
    setTimeout(() => {
      toast.value.show = false
    }, 3000)
  }

  function hideToast() {
    toast.value.show = false
  }

  return {
    loading,
    toast,
    setLoading,
    showToast,
    hideToast
  }
})
