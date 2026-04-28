<template>
  <div
    v-if="visible"
    class="mention-popup"
    :style="popupStyle"
    @click.stop
  >
    <div class="popup-header">
      <span class="title">@ 提及</span>
      <input
        v-model="searchKeyword"
        type="text"
        placeholder="搜索用户..."
        class="search-input"
        @input="handleSearch"
        @keydown="handleKeydown"
        ref="searchInputRef"
      />
    </div>

    <div class="user-list" ref="userListRef">
      <div
        v-for="(user, index) in users"
        :key="user.id"
        class="user-item"
        :class="{
          'active': index === activeIndex,
          'is-self': user.id === currentUserId
        }"
        @click="selectUser(user)"
        @mouseenter="activeIndex = index"
      >
        <img
          v-if="user.avatar"
          :src="user.avatar"
          class="user-avatar"
          alt="头像"
        />
        <div v-else class="user-avatar user-avatar-default">
          {{ user.username?.charAt(0) }}
        </div>
        <span class="user-name">{{ user.username }}</span>
        <span v-if="user.followerCount" class="follower-count">{{ user.followerCount }}粉丝</span>
        <span v-if="user.id === currentUserId" class="self-tag">自己</span>
      </div>

      <div v-if="users.length === 0 && !loading" class="empty-tip">
        {{ searchKeyword ? '未找到匹配的用户' : '暂无关注的用户' }}
      </div>

      <div v-if="loading" class="loading-tip">搜索中...</div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, computed, onMounted, onBeforeUnmount } from 'vue'
import { searchFollowingUsers } from '@/api/mention'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  visible: Boolean,
  position: {
    type: Object,
    default: () => ({ top: 0, left: 0 })
  }
})

const emit = defineEmits(['select', 'close'])

const appStore = useAppStore()
const userStore = useUserStore()
const searchKeyword = ref('')
const users = ref([])
const loading = ref(false)
const searchInputRef = ref(null)
const userListRef = ref(null)
const activeIndex = ref(0)
let searchTimer = null
let abortController = null

const currentUserId = computed(() => userStore.userInfo?.id)

const popupStyle = computed(() => {
  const popupWidth = 320
  const popupHeight = 400
  let top = props.position.top
  let left = props.position.left

  if (top < 10) {
    top = props.position.top + popupHeight + 30
  }
  if (top + popupHeight > window.innerHeight) {
    top = window.innerHeight - popupHeight - 10
  }
  if (left + popupWidth > window.innerWidth) {
    left = window.innerWidth - popupWidth - 10
  }
  if (left < 10) {
    left = 10
  }

  return {
    top: top + 'px',
    left: left + 'px'
  }
})

watch(() => props.visible, async (newVal) => {
  if (newVal) {
    searchKeyword.value = ''
    users.value = []
    activeIndex.value = 0
    await nextTick()
    searchInputRef.value?.focus()
    loadUsers()
  } else {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
  }
})

const handleSearch = () => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = setTimeout(() => {
    activeIndex.value = 0
    loadUsers()
  }, 300)
}

const loadUsers = async () => {
  loading.value = true
  try {
    if (abortController) {
      abortController.abort()
    }
    const res = await searchFollowingUsers(searchKeyword.value)
    if (res.code === 200) {
      users.value = res.data || []
    }
  } catch (error) {
    if (error.name !== 'AbortError' && error.name !== 'CanceledError') {
      console.error('搜索用户失败:', error)
    }
  } finally {
    loading.value = false
  }
}

const handleKeydown = (event) => {
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    activeIndex.value = (activeIndex.value + 1) % users.value.length
    scrollToActive()
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    activeIndex.value = (activeIndex.value - 1 + users.value.length) % users.value.length
    scrollToActive()
  } else if (event.key === 'Enter') {
    event.preventDefault()
    if (users.value.length > 0 && activeIndex.value >= 0) {
      selectUser(users.value[activeIndex.value])
    }
  } else if (event.key === 'Escape') {
    event.preventDefault()
    emit('close')
  }
}

const scrollToActive = () => {
  nextTick(() => {
    const activeItem = userListRef.value?.children[activeIndex.value]
    if (activeItem) {
      activeItem.scrollIntoView({ block: 'nearest' })
    }
  })
}

const selectUser = (user) => {
  if (user.id === currentUserId.value) {
    appStore.showToast('不能@自己', 'error')
    return
  }
  emit('select', user)
  emit('close')
}

const handleClickOutside = (event) => {
  if (props.visible && !event.target.closest('.mention-popup')) {
    emit('close')
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
})
</script>

<style scoped>
.mention-popup {
  position: fixed;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  width: 320px;
  max-height: 400px;
  z-index: 1000;
  overflow: hidden;
}

.popup-header {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.title {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #667eea;
}

.user-list {
  max-height: 320px;
  overflow-y: auto;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.user-item:hover,
.user-item.active {
  background: #f0f2ff;
}

.user-item.is-self {
  opacity: 0.5;
  cursor: not-allowed;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.user-avatar-default {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
}

.user-name {
  font-size: 14px;
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.self-tag {
  font-size: 11px;
  color: #999;
  background: #f5f5f5;
  padding: 1px 6px;
  border-radius: 8px;
}

.empty-tip,
.loading-tip {
  padding: 20px;
  text-align: center;
  color: #999;
  font-size: 13px;
}
</style>
