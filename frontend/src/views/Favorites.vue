<template>
  <div class="favorites-container">
    <div class="favorites-header">
      <h1>我的收藏</h1>
    </div>
    
    <div v-if="loading" class="loading">
      加载中...
    </div>
    
    <div v-else-if="favorites.length === 0" class="empty">
      <p>暂无收藏</p>
    </div>
    
    <div v-else class="favorites-list">
      <div
        v-for="post in favorites"
        :key="post.id"
        class="post-card"
        @click="goToPost(post.id)"
      >
        <div class="post-header">
          <div class="author-info">
            <img :src="post.authorAvatar || '/default-avatar.png'" class="avatar" />
            <div class="author-details">
              <div class="author-name">{{ post.authorUsername }}</div>
              <div class="post-time">{{ formatTime(post.createTime) }}</div>
            </div>
          </div>
        </div>
        
        <div class="post-content">
          <h3 class="post-title">{{ post.title }}</h3>
          <p class="post-preview">{{ getPlainTextPreview(post.content) }}</p>
        </div>
        
        <div class="post-footer">
          <div class="post-stats">
            <span class="stat-item">
              <span class="icon">❤️</span>
              {{ post.likeCount || 0 }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getFavoritePosts } from '@/api/post'
import { formatTime, getPlainTextPreview } from '@/utils/helpers'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const appStore = useAppStore()

const favorites = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(15)
const hasMore = ref(true)

async function loadFavorites(reset = true) {
  if (reset) {
    currentPage.value = 1
    favorites.value = []
  }
  
  loading.value = true
  
  try {
    const res = await getFavoritePosts({ 
      page: currentPage.value, 
      size: pageSize.value 
    })
    
    if (res.code === 200 && res.data) {
      const newPosts = res.data.content || []
      
      if (reset) {
        favorites.value = newPosts
      } else {
        favorites.value = [...favorites.value, ...newPosts]
      }
      
      hasMore.value = res.data.hasMore || false
    }
  } catch (error) {
    console.error('加载收藏失败:', error)
    appStore.showToast(error.message || '加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function goToPost(postId) {
  router.push(`/post/${postId}`)
}

onMounted(() => {
  loadFavorites()
})
</script>

<style scoped>
.favorites-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.favorites-header {
  margin-bottom: 30px;
}

.favorites-header h1 {
  font-size: 32px;
  color: #333;
}

.loading,
.empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 16px;
}

.favorites-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.post-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s;
}

.post-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.author-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.post-time {
  font-size: 12px;
  color: #999;
}

.post-content {
  margin-bottom: 16px;
}

.post-title {
  font-size: 18px;
  color: #333;
  margin-bottom: 8px;
  line-height: 1.4;
}

.post-preview {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.post-footer {
  display: flex;
  justify-content: flex-end;
}

.post-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #666;
}

.stat-item .icon {
  font-size: 16px;
}
</style>
