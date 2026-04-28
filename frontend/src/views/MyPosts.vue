<template>
  <div class="my-posts-container">
    <div class="my-posts-header">
      <h1>我的帖子</h1>
      <button class="create-btn" @click="goToEditor">
        <span>+</span> 创建帖子
      </button>
    </div>
    
    <div v-if="loading" class="loading">
      加载中...
    </div>
    
    <div v-else-if="posts.length === 0" class="empty">
      <p>暂无帖子</p>
      <button class="create-post-btn" @click="goToEditor">创建第一个帖子</button>
    </div>
    
    <div v-else class="posts-list">
      <div
        v-for="post in posts"
        :key="post.id"
        class="post-card"
      >
        <div class="post-header">
          <h3 class="post-title">{{ post.title }}</h3>
          <div class="post-actions">
            <button class="action-btn delete-btn" @click="deletePost(post.id)">
              删除
            </button>
          </div>
        </div>
        
        <p class="post-preview">{{ getPlainTextPreview(post.content) }}</p>
        
        <div class="post-footer">
          <div class="post-info">
            <span class="post-time">{{ formatDateTime(post.createdAt) }}</span>
            <span class="post-stats">
              ❤️ {{ post.likeCount || 0 }} · ⭐ {{ post.favoriteCount || 0 }}
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
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { getPostList, deletePost as deletePostApi } from '@/api/post'
import { getPlainTextPreview, formatDateTime } from '@/utils/helpers'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const posts = ref([])
const loading = ref(false)

async function loadPosts() {
  loading.value = true
  
  try {
    const res = await getPostList({ page: 1, pageSize: 20 })
    if (res.code === 200) {
      posts.value = (res.data.list || []).filter(p => p.authorId === userStore.user?.id)
    }
  } catch (error) {
    console.error('加载帖子失败:', error)
    appStore.showToast('加载帖子失败', 'error')
  } finally {
    loading.value = false
  }
}

async function deletePost(postId) {
  if (!confirm('确定要删除这个帖子吗？')) return
  
  try {
    const res = await deletePostApi(postId)
    if (res.code === 200) {
      appStore.showToast('删除成功')
      posts.value = posts.value.filter(p => p.id !== postId)
    }
  } catch (error) {
    console.error('删除帖子失败:', error)
    appStore.showToast('删除帖子失败', 'error')
  }
}

function goToEditor() {
  router.push('/post-editor')
}

onMounted(() => {
  loadPosts()
})
</script>

<style scoped>
.my-posts-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.my-posts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.my-posts-header h1 {
  font-size: 32px;
  color: #333;
}

.create-btn,
.create-post-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.create-btn:hover,
.create-post-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
}

.loading,
.empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 16px;
}

.empty p {
  margin-bottom: 20px;
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.post-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.post-title {
  font-size: 18px;
  color: #333;
  margin: 0;
  flex: 1;
  line-height: 1.4;
}

.post-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.delete-btn {
  background: #fee2e2;
  color: #dc2626;
}

.delete-btn:hover {
  background: #fecaca;
}

.post-preview {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 16px;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.post-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}

.post-time {
  color: #999;
}

.post-stats {
  color: #667eea;
  font-weight: 500;
}
</style>
