<template>
  <div class="mentions-page">
    <div class="page-header">
      <h2>@我的</h2>
      <p class="subtitle">查看谁在帖子或评论中提到了你</p>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="mentions.length === 0" class="empty-state">
      <div class="empty-icon">@</div>
      <p>还没有人@你</p>
    </div>

    <div v-else class="mention-list">
      <div 
        v-for="mention in mentions" 
        :key="mention.id" 
        class="mention-item"
        :class="{ 'unread': !mention.isRead }"
      >
        <div class="mention-header">
          <div class="mentioner-info" @click="goToProfile(mention.mentionerId)">
            <img 
              v-if="mention.mentionerAvatar" 
              :src="mention.mentionerAvatar" 
              class="avatar"
              alt="头像"
            />
            <div v-else class="avatar avatar-default">{{ mention.mentionerUsername?.charAt(0) }}</div>
            <span class="username">{{ mention.mentionerUsername }}</span>
          </div>
          <span class="time">{{ formatTime(mention.createTime) }}</span>
        </div>

        <div class="mention-content" @click="goToPost(mention.postId, mention.commentId)">
          <div class="post-title">
            <i class="icon">📝</i>
            {{ mention.postTitle }}
          </div>
          <div class="content-preview">{{ mention.contentPreview }}</div>
        </div>

        <div class="mention-actions">
          <button class="btn-link" @click="handleDelete(mention.id)">删除</button>
        </div>
      </div>
    </div>

    <div v-if="hasMore && !loading" class="load-more">
      <button @click="loadMore" :disabled="loadingMore">
        {{ loadingMore ? '加载中...' : '加载更多' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyMentions, markMentionAsRead, deleteMention } from '@/api/mention'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const appStore = useAppStore()

const mentions = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(1)
const hasMore = ref(true)

const loadMentions = async (isLoadMore = false) => {
  if (isLoadMore) {
    loadingMore.value = true
  } else {
    loading.value = true
  }

  try {
    const res = await getMyMentions(page.value, 20)
    if (res.code === 200) {
      const data = res.data
      if (isLoadMore) {
        mentions.value = [...mentions.value, ...data.content]
      } else {
        mentions.value = data.content
      }
      hasMore.value = data.hasMore
    }
  } catch (error) {
    appStore.showToast('加载失败', 'error')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  page.value++
  loadMentions(true)
}

const handleDelete = async (id) => {
  if (!confirm('确定要删除这条@通知吗？')) return
  
  try {
    const res = await deleteMention(id)
    if (res.code === 200) {
      appStore.showToast('删除成功')
      mentions.value = mentions.value.filter(m => m.id !== id)
    }
  } catch (error) {
    appStore.showToast('删除失败', 'error')
  }
}

const goToPost = async (postId, commentId) => {
  const mention = mentions.value.find(m => m.postId === postId && (!commentId || m.commentId === commentId))
  if (mention && !mention.isRead) {
    try {
      await markMentionAsRead(mention.id)
      mention.isRead = true
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
  
  if (commentId) {
    router.push(`/post/${postId}?comment=${commentId}`)
  } else {
    router.push(`/post/${postId}`)
  }
}

const goToProfile = (userId) => {
  router.push(`/profile/${userId}`)
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadMentions()
})
</script>

<style scoped>
.mentions-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.subtitle {
  color: #999;
  font-size: 14px;
  margin: 0;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #999;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.3;
}

.mention-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mention-item {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.2s;
}

.mention-item.unread {
  border-left: 3px solid #667eea;
  background: #f8f9ff;
}

.mention-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.mention-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.mentioner-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-default {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
}

.username {
  font-weight: 500;
  color: #333;
}

.time {
  font-size: 12px;
  color: #999;
}

.mention-content {
  cursor: pointer;
  margin-bottom: 12px;
}

.post-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #667eea;
  font-weight: 500;
  margin-bottom: 8px;
}

.icon {
  font-style: normal;
}

.content-preview {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mention-actions {
  display: flex;
  justify-content: flex-end;
}

.btn-link {
  background: none;
  border: none;
  color: #999;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  transition: color 0.2s;
}

.btn-link:hover {
  color: #ff4d4f;
}

.load-more {
  text-align: center;
  margin-top: 20px;
}

.load-more button {
  padding: 10px 32px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 20px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.load-more button:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
}

.load-more button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
