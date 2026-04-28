<template>
  <div class="message-tab-container">
    <!-- 内容头部 -->
    <div class="content-header">
      <h2>{{ title }}</h2>
      <button 
        v-if="messageStore.currentMessages.length > 0"
        class="mark-all-read-btn"
        @click="handleMarkAllAsRead"
      >
        全部标记为已读
      </button>
    </div>

    <!-- 加载状态 -->
    <div v-if="messageStore.loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="messageStore.currentMessages.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
        <polyline points="22,6 12,13 2,6"></polyline>
      </svg>
      <h3>{{ emptyText }}</h3>
      <p>{{ emptySubtext }}</p>
    </div>

    <!-- 消息列表 -->
    <div v-else class="messages-list" ref="messagesListRef" @scroll="handleScroll">
      <div 
        v-for="msg in messageStore.currentMessages" 
        :key="msg.notificationId || msg.id"
        class="message-item"
        @click="handleMessageClick(msg)"
      >
        <div class="message-avatar">
          <div class="avatar-wrapper">
            <img 
              v-if="msg.avatar || msg.replyAuthorAvatar" 
              :src="msg.avatar || msg.replyAuthorAvatar" 
              :alt="msg.username || msg.replyAuthorUsername"
            />
            <div v-else class="avatar-placeholder">
              {{ (msg.username || msg.replyAuthorUsername || 'U').charAt(0).toUpperCase() }}
            </div>
          </div>
        </div>
        
        <div class="message-content">
          <div class="message-header">
            <span class="message-user">{{ escapeHtml(msg.username || msg.replyAuthorUsername) }}</span>
            <span class="message-action">{{ escapeHtml(msg.action || '回复了你') }}</span>
          </div>
          
          <div class="message-body">
            <div v-if="msg.replyContent" class="reply-content">
              {{ escapeHtml(msg.replyContent) }}
            </div>
            <div v-if="msg.parentContent" class="parent-comment-preview">
              回复了你的评论：{{ escapeHtml(msg.parentContent).substring(0, 100) }}...
            </div>
            <div v-if="msg.sourceTitle" class="message-source">
              {{ escapeHtml(msg.sourceTitle) }}
            </div>
          </div>
          
          <div class="message-meta">
            <span class="message-time">{{ formatTime(msg.createTime || msg.time) }}</span>
            <button 
              class="delete-btn"
              @click.stop="handleDelete(msg.notificationId || msg.id)"
            >
              删除
            </button>
          </div>
        </div>
        
        <div v-if="msg.thumbnail" class="message-thumbnail">
          <img :src="msg.thumbnail" alt="缩略图" />
        </div>
      </div>

      <!-- 加载更多 -->
      <div v-if="messageStore.hasMore" class="load-more">
        <button 
          v-if="!messageStore.loadingMore"
          @click="handleLoadMore"
          class="load-more-btn"
        >
          加载更多
        </button>
        <div v-else class="loading-more">
          <div class="spinner-small"></div>
          <span>加载中...</span>
        </div>
      </div>

      <!-- 没有更多 -->
      <div v-else-if="messageStore.currentMessages.length > 0" class="no-more">
        没有更多了
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessageStore } from '@/stores/message'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const messageStore = useMessageStore()
const appStore = useAppStore()
const messagesListRef = ref(null)

// 根据路由确定消息类型
const messageType = computed(() => {
  const pathMap = {
    '/messages/likes': 'likes',
    '/messages/replies': 'replies',
    '/messages/mentions': 'mentions',
    '/messages/likes-received': 'likes-received',
    '/messages/system': 'system'
  }
  return pathMap[route.path] || 'likes'
})

// 监听路由变化，同步更新store中的currentTab
watch(
  () => route.path,
  (newPath) => {
    if (newPath.includes('/messages/')) {
      const tab = messageType.value
      messageStore.setCurrentTab(tab)
      loadMessages()
    }
  }
)

// 标题配置
const titleConfig = {
  'likes': '我的消息',
  'replies': '回复我的',
  'mentions': '@我的',
  'likes-received': '收到的赞',
  'system': '系统通知'
}

const title = computed(() => titleConfig[messageType.value] || '消息')

// 空状态文本
const emptyTextConfig = {
  'likes': '暂无消息',
  'replies': '暂无回复消息',
  'mentions': '暂无@消息',
  'likes-received': '暂无收到的赞',
  'system': '暂无系统通知'
}

const emptySubtextConfig = {
  'likes': '当有人与你互动时，这里会显示',
  'replies': '当有人回复你的评论时，这里会显示',
  'mentions': '当有人@你时，这里会显示',
  'likes-received': '当有人点赞你的内容时，这里会显示',
  'system': '系统通知会显示在这里'
}

const emptyText = computed(() => emptyTextConfig[messageType.value] || '暂无消息')
const emptySubtext = computed(() => emptySubtextConfig[messageType.value] || '')

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  
  if (diff < minute) {
    return '刚刚'
  } else if (diff < hour) {
    return Math.floor(diff / minute) + '分钟前'
  } else if (diff < day) {
    return Math.floor(diff / hour) + '小时前'
  } else if (diff < 7 * day) {
    return Math.floor(diff / day) + '天前'
  } else {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  }
}

// HTML转义
const escapeHtml = (text) => {
  if (!text) return ''
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

// 加载消息
const loadMessages = async () => {
  try {
    await messageStore.loadMessages(messageType.value, true)
  } catch (error) {
    appStore.showToast('加载消息失败', 'error')
  }
}

// 加载更多
const handleLoadMore = async () => {
  try {
    await messageStore.loadMore(messageType.value)
  } catch (error) {
    appStore.showToast('加载更多失败', 'error')
  }
}

// 滚动处理（无限滚动）
const handleScroll = (e) => {
  const { scrollTop, scrollHeight, clientHeight } = e.target
  // 距离底部100px时加载更多
  if (scrollHeight - scrollTop - clientHeight < 100 && messageStore.hasMore && !messageStore.loadingMore) {
    handleLoadMore()
  }
}

// 点击消息
const handleMessageClick = (msg) => {
  // 如果是回复消息，跳转到帖子并定位到评论
  if (msg.postId && msg.replyId) {
    router.push(`/post/${msg.postId}?commentId=${msg.replyId}`)
  }
  // TODO: 其他类型消息的跳转逻辑
}

// 删除消息
const handleDelete = async (notificationId) => {
  if (confirm('确定要删除这条通知吗？')) {
    try {
      await messageStore.removeNotification(notificationId, messageType.value)
      appStore.showToast('删除成功')
    } catch (error) {
      appStore.showToast('删除失败', 'error')
    }
  }
}

// 全部标记为已读
const handleMarkAllAsRead = async () => {
  try {
    await messageStore.markAllAsRead(messageType.value)
    appStore.showToast('已全部标记为已读')
  } catch (error) {
    appStore.showToast('操作失败', 'error')
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadMessages()
})
</script>

<style scoped>
.message-tab-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.content-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.mark-all-read-btn {
  padding: 6px 16px;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  transition: all 0.3s;
}

.mark-all-read-btn:hover {
  border-color: #667eea;
  color: #667eea;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #909399;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: #909399;
}

.empty-state svg {
  width: 80px;
  height: 80px;
  margin-bottom: 20px;
  opacity: 0.3;
}

.empty-state h3 {
  font-size: 16px;
  font-weight: 500;
  color: #606266;
  margin: 0 0 8px 0;
}

.empty-state p {
  font-size: 14px;
  margin: 0;
}

/* 消息列表 */
.messages-list {
  flex: 1;
  overflow-y: auto;
}

.message-item {
  display: flex;
  gap: 16px;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.3s;
  cursor: pointer;
}

.message-item:hover {
  background: #f5f7fa;
}

.message-avatar {
  flex-shrink: 0;
}

.avatar-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid #e4e7ed;
}

.avatar-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
  font-size: 18px;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.message-user {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.message-action {
  color: #909399;
  font-size: 13px;
}

.message-body {
  margin-bottom: 8px;
}

.reply-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  margin-bottom: 6px;
}

.parent-comment-preview {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-source {
  font-size: 13px;
  color: #606266;
  background: #f5f7fa;
  padding: 6px 10px;
  border-radius: 4px;
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.message-time {
  font-size: 12px;
  color: #c0c4cc;
}

.delete-btn {
  padding: 4px 12px;
  background: transparent;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  color: #909399;
  transition: all 0.3s;
  opacity: 0;
}

.message-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  border-color: #f56c6c;
  color: #f56c6c;
}

.message-thumbnail {
  flex-shrink: 0;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
}

.message-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 加载更多 */
.load-more {
  padding: 20px;
  text-align: center;
}

.load-more-btn {
  padding: 10px 32px;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: all 0.3s;
}

.load-more-btn:hover {
  border-color: #667eea;
  color: #667eea;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #909399;
  font-size: 14px;
}

.spinner-small {
  width: 20px;
  height: 20px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.no-more {
  padding: 20px;
  text-align: center;
  color: #c0c4cc;
  font-size: 14px;
}
</style>
