<template>
  <div class="messages-container">
    <!-- 顶部导航栏 -->
    <div class="top-bar">
      <div class="top-bar-left">
        <button class="back-btn" @click="goBack">← 返回</button>
        <div class="top-bar-title">消息中心</div>
      </div>
      <div class="top-bar-right">
        <div class="user-avatar-top" @click="goToProfile" title="个人中心">
          <div class="user-avatar-placeholder-top">
            {{ userStore.userInfo?.username?.charAt(0).toUpperCase() || 'U' }}
          </div>
        </div>
      </div>
    </div>

    <!-- 主容器 -->
    <div class="container">
      <div class="messages-layout">
        <!-- 左侧边栏 -->
        <aside class="sidebar">
          <div class="sidebar-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
              <polyline points="22,6 12,13 2,6"></polyline>
            </svg>
            消息中心
          </div>
          
          <nav class="menu-list">
            <router-link 
              v-for="tab in tabs" 
              :key="tab.type"
              :to="`/messages/${tab.type}`"
              class="menu-item"
              active-class="active"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" v-html="tab.icon"></svg>
              <span>{{ tab.name }}</span>
              <span v-if="getUnreadCount(tab.type) > 0" class="unread-badge">
                {{ getUnreadCount(tab.type) }}
              </span>
            </router-link>
            
            <div class="menu-divider"></div>
            
            <router-link 
              to="/messages/settings"
              class="menu-item"
              active-class="active"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="3"></circle>
                <path d="M12 1v6M12 17v6M4.22 4.22l4.24 4.24M15.54 15.54l4.24 4.24M1 12h6M17 12h6M4.22 19.78l4.24-4.24M15.54 8.46l4.24-4.24"></path>
              </svg>
              <span>消息设置</span>
            </router-link>
          </nav>
        </aside>

        <!-- 右侧主内容 -->
        <main class="main-content">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <keep-alive>
                <component :is="Component" :key="$route.path" />
              </keep-alive>
            </transition>
          </router-view>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { onMounted, watch } from 'vue'

const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()

// 标签配置
const tabs = [
  {
    type: 'likes',
    name: '我的消息',
    icon: '<path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>'
  },
  {
    type: 'replies',
    name: '回复我的',
    icon: '<path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>'
  },
  {
    type: 'mentions',
    name: '@我的',
    icon: '<circle cx="12" cy="12" r="10"></circle><path d="M16 12l-4-4-4 4M12 16V8"></path>'
  },
  {
    type: 'likes-received',
    name: '收到的赞',
    icon: '<path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"></path>'
  },
  {
    type: 'system',
    name: '系统通知',
    icon: '<circle cx="12" cy="12" r="3"></circle><path d="M12 1v6M12 17v6M4.22 4.22l4.24 4.24M15.54 15.54l4.24 4.24M1 12h6M17 12h6M4.22 19.78l4.24-4.24M15.54 8.46l4.24-4.24"></path>'
  }
]

// 获取未读数量
const getUnreadCount = (type) => {
  return messageStore.unreadCounts[type] || 0
}

// 返回上一页
const goBack = () => {
  window.history.back()
}

// 跳转到个人主页
const goToProfile = () => {
  router.push('/profile')
}

// 监听路由变化，同步更新store的currentTab
watch(
  () => router.currentRoute.value.path,
  (newPath) => {
    if (newPath.includes('/messages/')) {
      const pathMap = {
        '/messages/likes': 'likes',
        '/messages/replies': 'replies',
        '/messages/mentions': 'mentions',
        '/messages/likes-received': 'likes-received',
        '/messages/system': 'system',
        '/messages/settings': 'settings'
      }
      const tab = pathMap[newPath] || 'likes'
      messageStore.setCurrentTab(tab)
    }
  },
  { immediate: true }
)

// 组件挂载时获取未读数量
onMounted(() => {
  messageStore.fetchUnreadCount()
})
</script>

<style scoped>
.messages-container {
  min-height: 100vh;
  background: #f5f7fa;
}

/* 顶部导航栏 */
.top-bar {
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  padding: 16px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 100;
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.back-btn {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: all 0.3s;
}

.back-btn:hover {
  border-color: #667eea;
  color: #667eea;
}

.top-bar-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.user-avatar-top {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid #e4e7ed;
  transition: all 0.3s;
}

.user-avatar-top:hover {
  border-color: #667eea;
  transform: scale(1.05);
}

.user-avatar-placeholder-top {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
  font-size: 16px;
}

/* 主容器 */
.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.messages-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  min-height: calc(100vh - 140px);
}

/* 左侧边栏 */
.sidebar {
  background: #fafafa;
  border-right: 1px solid #e4e7ed;
  padding: 20px 0;
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px 20px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #e4e7ed;
}

.sidebar-title svg {
  width: 20px;
  height: 20px;
}

.menu-list {
  padding: 10px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  color: #606266;
  text-decoration: none;
  transition: all 0.3s;
  position: relative;
  cursor: pointer;
}

.menu-item:hover {
  background: #ecf5ff;
  color: #667eea;
}

.menu-item.active {
  background: #ecf5ff;
  color: #667eea;
  font-weight: 500;
}

.menu-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: #667eea;
}

.menu-item svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.unread-badge {
  margin-left: auto;
  background: #f56c6c;
  color: white;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.menu-divider {
  height: 1px;
  background: #e4e7ed;
  margin: 12px 20px;
}

/* 右侧主内容 */
.main-content {
  display: flex;
  flex-direction: column;
  background: white;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .container {
    padding: 10px;
  }

  .messages-layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    display: none;
  }

  .top-bar {
    padding: 12px 20px;
  }
}
</style>
