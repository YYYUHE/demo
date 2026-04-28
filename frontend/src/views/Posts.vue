<template>
  <div class="posts-container">
    <div class="posts-header">
      <h1>发现</h1>
      <button class="create-btn" @click="goToEditor">
        <span>+</span> 创建帖子
      </button>
    </div>
    
    <div class="posts-content">
      <div class="posts-list">
        <div v-if="loading && posts.length === 0" class="loading">
          加载中...
        </div>
        
        <div v-else-if="posts.length === 0" class="empty">
          <p>暂无帖子</p>
        </div>
        
        <div v-else>
          <div
            v-for="post in posts"
            :key="post.id"
            class="post-card"
            @click="goToPostDetail(post.id)"
          >
            <div class="post-header">
              <div class="author-info">
                <img :src="post.authorAvatar || '/default-avatar.png'" class="avatar" />
                <div class="author-details">
                  <div class="author-name">{{ post.authorName }}</div>
                  <div class="post-time">{{ formatTime(post.createdAt) }}</div>
                </div>
              </div>
            </div>
            
            <div class="post-content">
              <h3 class="post-title">{{ post.title }}</h3>
              <p class="post-preview">{{ getPlainTextPreview(post.content) }}</p>
            </div>
            
            <div v-if="post.topics && post.topics.length > 0" class="post-topics">
              <span v-for="topic in post.topics" :key="topic" class="topic-tag">
                #{{ topic }}
              </span>
            </div>
            
            <div class="post-footer">
              <div class="post-stats">
                <span class="stat-item">
                  <span class="icon">❤️</span>
                  {{ post.likeCount || 0 }}
                </span>
                <span class="stat-item">
                  <span class="icon">⭐</span>
                  {{ post.favoriteCount || 0 }}
                </span>
              </div>
            </div>
          </div>
          
          <div v-if="hasMore" class="load-more" @click="loadMore">
            {{ loading ? '加载中...' : '加载更多' }}
          </div>
        </div>
      </div>
      
      <div class="sidebar">
        <div class="sidebar-section">
          <h3>热门话题</h3>
          <div v-if="popularTopics.length > 0" class="topics-list">
            <div
              v-for="topic in popularTopics"
              :key="topic.name"
              class="topic-item"
              @click="filterByTopic(topic.name)"
            >
              <span class="topic-name">#{{ topic.name }}</span>
              <span class="topic-count">{{ topic.usageCount }}</span>
            </div>
          </div>
          <div v-else class="empty-topics">
            暂无热门话题
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPostList } from '@/api/post'
import { getPopularTopics } from '@/api/topic'
import { formatTime, getPlainTextPreview } from '@/utils/helpers'

const router = useRouter()

const posts = ref([])
const popularTopics = ref([])
const loading = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = 15

async function loadPosts(page = 1) {
  if (loading.value) return
  
  loading.value = true
  
  try {
    const res = await getPostList({
      page,
      pageSize
    })
    
    if (res.code === 200) {
      if (page === 1) {
        posts.value = res.data.list || []
      } else {
        posts.value = [...posts.value, ...(res.data.list || [])]
      }
      
      hasMore.value = (res.data.list || []).length >= pageSize
    }
  } catch (error) {
    console.error('加载帖子失败:', error)
  } finally {
    loading.value = false
  }
}

async function loadPopularTopics() {
  try {
    const res = await getPopularTopics({ limit: 10 })
    if (res.code === 200) {
      popularTopics.value = res.data || []
    }
  } catch (error) {
    console.error('加载热门话题失败:', error)
  }
}

function loadMore() {
  if (!hasMore.value || loading.value) return
  currentPage.value++
  loadPosts(currentPage.value)
}

function goToEditor() {
  router.push('/post-editor')
}

function goToPostDetail(postId) {
  router.push(`/post/${postId}`)
}

function filterByTopic(topicName) {
  console.log('Filter by topic:', topicName)
}

onMounted(() => {
  loadPosts()
  loadPopularTopics()
})
</script>

<style scoped>
.posts-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.posts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.posts-header h1 {
  font-size: 32px;
  color: #333;
}

.create-btn {
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

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
}

.posts-content {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 30px;
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.loading,
.empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 16px;
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

.post-topics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.topic-tag {
  padding: 4px 12px;
  background: #f0f2f5;
  color: #667eea;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.load-more {
  text-align: center;
  padding: 16px;
  color: #667eea;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.load-more:hover {
  background: #f0f2f5;
  border-radius: 8px;
}

.sidebar {
  position: sticky;
  top: 20px;
  height: fit-content;
}

.sidebar-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.sidebar-section h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 16px;
  font-weight: 600;
}

.topics-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.topic-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.topic-item:hover {
  background: #f0f2f5;
}

.topic-name {
  font-size: 14px;
  color: #667eea;
  font-weight: 500;
}

.topic-count {
  font-size: 12px;
  color: #999;
}

.empty-topics {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 14px;
}

@media (max-width: 768px) {
  .posts-content {
    grid-template-columns: 1fr;
  }
  
  .sidebar {
    display: none;
  }
}
</style>
