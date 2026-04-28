<template>
  <div class="profile-container">
    <div class="profile-header">
      <div class="avatar-section">
        <div class="avatar-wrapper">
          <img :src="userProfile?.avatar || '/default-avatar.png'" class="avatar" />
          <label class="avatar-upload">
            <input type="file" accept="image/*" @change="handleAvatarUpload" />
            <span>更换头像</span>
          </label>
        </div>
      </div>
      
      <div class="profile-info">
        <h1 class="username">{{ userProfile?.username || '未设置' }}</h1>
        <p class="email">{{ userProfile?.email || '未设置' }}</p>
        <div class="stats">
          <div class="stat-item" @click="goToMessages" style="cursor: pointer;">
            <span class="stat-value">{{ messageStore.totalUnread || 0 }}</span>
            <span class="stat-label">消息</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ userProfile?.postCount || 0 }}</span>
            <span class="stat-label">帖子</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ userProfile?.followerCount || 0 }}</span>
            <span class="stat-label">粉丝</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ userProfile?.followingCount || 0 }}</span>
            <span class="stat-label">关注</span>
          </div>
        </div>
      </div>
    </div>
    
    <div class="profile-content">
      <div class="tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'posts' }"
          @click="activeTab = 'posts'"
        >
          我的帖子
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'favorites' }"
          @click="activeTab = 'favorites'"
        >
          我的收藏
        </button>
      </div>
      
      <div class="tab-content">
        <div v-if="activeTab === 'posts'" class="posts-tab">
          <div v-if="loading" class="loading">加载中...</div>
          <div v-else-if="posts.length === 0" class="empty">暂无帖子</div>
          <div v-else class="posts-list">
            <div
              v-for="post in posts"
              :key="post.id"
              class="post-item"
              @click="goToPost(post.id)"
            >
              <h3 class="post-title">{{ post.title }}</h3>
              <p class="post-preview">{{ getPlainTextPreview(post.content) }}</p>
              <div class="post-meta">
                <span class="post-time">{{ formatDateTime(post.createdAt) }}</span>
                <span class="post-stats">
                  ❤️ {{ post.likeCount || 0 }} · ⭐ {{ post.favoriteCount || 0 }}
                </span>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="activeTab === 'favorites'" class="favorites-tab">
          <div v-if="loading" class="loading">加载中...</div>
          <div v-else-if="favorites.length === 0" class="empty">暂无收藏</div>
          <div v-else class="favorites-list">
            <div
              v-for="post in favorites"
              :key="post.id"
              class="post-item"
              @click="goToPost(post.id)"
            >
              <h3 class="post-title">{{ post.title }}</h3>
              <p class="post-preview">{{ getPlainTextPreview(post.content) }}</p>
              <div class="post-meta">
                <span class="post-time">{{ formatDateTime(post.createdAt) }}</span>
                <span class="post-stats">
                  ❤️ {{ post.likeCount || 0 }} · ⭐ {{ post.favoriteCount || 0 }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { useMessageStore } from '@/stores/message'
import { updateAvatar } from '@/api/auth'
import { getPostList } from '@/api/post'
import { getPlainTextPreview, formatDateTime, fileToBase64 } from '@/utils/helpers'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()
const messageStore = useMessageStore()

const activeTab = ref('posts')
const loading = ref(false)
const posts = ref([])
const favorites = ref([])

const userProfile = ref(null)

async function loadUserProfile() {
  try {
    const profile = await userStore.loadUserProfile()
    userProfile.value = profile
  } catch (error) {
    console.error('加载用户资料失败:', error)
  }
}

async function loadPosts() {
  loading.value = true
  
  try {
    const res = await getPostList({ page: 1, pageSize: 20 })
    if (res.code === 200) {
      posts.value = (res.data.list || []).filter(p => p.authorId === userStore.user?.id)
    }
  } catch (error) {
    console.error('加载帖子失败:', error)
  } finally {
    loading.value = false
  }
}

async function loadFavorites() {
  loading.value = true
  
  try {
    const res = await getPostList({ page: 1, pageSize: 20, favorites: true })
    if (res.code === 200) {
      favorites.value = res.data.list || []
    }
  } catch (error) {
    console.error('加载收藏失败:', error)
  } finally {
    loading.value = false
  }
}

async function handleAvatarUpload(event) {
  const file = event.target.files[0]
  if (!file) return
  
  try {
    const base64 = await fileToBase64(file)
    const res = await updateAvatar({ avatar: base64 })
    
    if (res.code === 200) {
      appStore.showToast('头像更新成功')
      userProfile.value.avatar = res.data.avatar
    }
  } catch (error) {
    console.error('更新头像失败:', error)
    appStore.showToast('更新头像失败', 'error')
  }
}

function goToPost(postId) {
  router.push(`/post/${postId}`)
}

function goToMessages() {
  router.push('/messages')
}

onMounted(async () => {
  await loadUserProfile()
  await messageStore.fetchUnreadCount()
  loadPosts()
})
</script>

<style scoped>
.profile-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.profile-header {
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.avatar-wrapper {
  position: relative;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #667eea;
}

.avatar-upload {
  position: absolute;
  bottom: 0;
  right: 0;
  background: #667eea;
  color: white;
  padding: 8px 12px;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.avatar-upload:hover {
  background: #5a67d8;
}

.avatar-upload input {
  display: none;
}

.profile-info {
  text-align: center;
}

.username {
  font-size: 28px;
  color: #333;
  margin-bottom: 8px;
}

.email {
  font-size: 14px;
  color: #999;
  margin-bottom: 24px;
}

.stats {
  display: flex;
  justify-content: center;
  gap: 40px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #999;
}

.profile-content {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  border-bottom: 1px solid #e5e7eb;
}

.tab-btn {
  padding: 12px 24px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.3s;
}

.tab-btn:hover {
  color: #667eea;
}

.tab-btn.active {
  color: #667eea;
  border-bottom-color: #667eea;
}

.loading,
.empty {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 14px;
}

.posts-list,
.favorites-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-item {
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.post-item:hover {
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
}

.post-title {
  font-size: 16px;
  color: #333;
  margin-bottom: 8px;
}

.post-preview {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
  line-height: 1.6;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}

.post-stats {
  color: #667eea;
}
</style>
