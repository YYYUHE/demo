import { defineStore } from 'pinia'
import {
  getReplyMessages,
  getLikeMessages,
  getMentionMessages,
  getLikesReceivedMessages,
  getSystemMessages,
  deleteNotification,
  markAsRead,
  markAllAsRead,
  getUnreadCount,
  getMessageSettings,
  updateMessageSettings
} from '@/api/message'

export const useMessageStore = defineStore('message', {
  state: () => ({
    // 当前选中的标签
    currentTab: 'likes',
    
    // 各类型消息数据（缓存）
    messages: {
      likes: [],
      replies: [],
      mentions: [],
      likesReceived: [],
      system: []
    },
    
    // 分页信息
    pagination: {
      likes: { page: 1, size: 20, total: 0, hasMore: true },
      replies: { page: 1, size: 20, total: 0, hasMore: true },
      mentions: { page: 1, size: 20, total: 0, hasMore: true },
      likesReceived: { page: 1, size: 20, total: 0, hasMore: true },
      system: { page: 1, size: 20, total: 0, hasMore: true }
    },
    
    // 加载状态
    loading: false,
    loadingMore: false,
    
    // 未读数量
    unreadCounts: {
      likes: 0,
      replies: 0,
      mentions: 0,
      likesReceived: 0,
      system: 0,
      total: 0
    },
    
    // 消息设置
    settings: {
      emailNotification: true,
      pushNotification: true,
      replyNotification: true,
      likeNotification: true,
      mentionNotification: true,
      systemNotification: true
    }
  }),

  getters: {
    // 获取当前标签的消息列表
    currentMessages: (state) => {
      return state.messages[state.currentTab] || []
    },
    
    // 获取当前标签的分页信息
    currentPagination: (state) => {
      return state.pagination[state.currentTab] || {}
    },
    
    // 是否有更多数据
    hasMore: (state) => {
      return state.pagination[state.currentTab]?.hasMore || false
    },
    
    // 总未读数
    totalUnread: (state) => {
      return state.unreadCounts.total
    }
  },

  actions: {
    /**
     * 设置当前标签
     */
    setCurrentTab(tab) {
      this.currentTab = tab
    },

    /**
     * 加载指定类型的消息
     * @param {string} type - 消息类型
     * @param {boolean} refresh - 是否刷新（清空缓存）
     */
    async loadMessages(type = this.currentTab, refresh = false) {
      // 如果已有数据且不刷新，直接返回
      if (!refresh && this.messages[type].length > 0) {
        return this.messages[type]
      }

      this.loading = true
      
      try {
        let response
        const params = { page: 1, size: 20 }
        
        switch (type) {
          case 'replies':
            response = await getReplyMessages(params)
            break
          case 'likes':
            response = await getLikeMessages(params)
            break
          case 'mentions':
            response = await getMentionMessages(params)
            break
          case 'likes-received':
            response = await getLikesReceivedMessages(params)
            break
          case 'system':
            response = await getSystemMessages(params)
            break
          default:
            response = { code: 200, data: { items: [], total: 0 } }
        }

        if (response.code === 200 && response.data) {
          const items = response.data.items || []
          const total = response.data.total || 0
          
          // 更新消息数据
          this.messages[type] = items
          
          // 更新分页信息
          this.pagination[type] = {
            page: 1,
            size: 20,
            total: total,
            hasMore: items.length < total
          }
          
          return items
        } else {
          this.messages[type] = []
          this.pagination[type] = { page: 1, size: 20, total: 0, hasMore: false }
          return []
        }
      } catch (error) {
        console.error(`加载${type}消息失败:`, error)
        throw error
      } finally {
        this.loading = false
      }
    },

    /**
     * 加载更多消息（无限滚动）
     */
    async loadMore(type = this.currentTab) {
      if (this.loadingMore || !this.hasMore) return

      this.loadingMore = true
      
      try {
        const currentPage = this.pagination[type].page
        const nextPage = currentPage + 1
        const params = { page: nextPage, size: 20 }
        
        let response
        
        switch (type) {
          case 'replies':
            response = await getReplyMessages(params)
            break
          case 'likes':
            response = await getLikeMessages(params)
            break
          case 'mentions':
            response = await getMentionMessages(params)
            break
          case 'likes-received':
            response = await getLikesReceivedMessages(params)
            break
          case 'system':
            response = await getSystemMessages(params)
            break
          default:
            response = { code: 200, data: { items: [], total: 0 } }
        }

        if (response.code === 200 && response.data) {
          const items = response.data.items || []
          const total = response.data.total || 0
          
          // 追加新数据
          this.messages[type] = [...this.messages[type], ...items]
          
          // 更新分页信息
          this.pagination[type] = {
            page: nextPage,
            size: 20,
            total: total,
            hasMore: this.messages[type].length < total
          }
        }
      } catch (error) {
        console.error(`加载更多${type}消息失败:`, error)
        throw error
      } finally {
        this.loadingMore = false
      }
    },

    /**
     * 删除通知
     */
    async removeNotification(notificationId, type = this.currentTab) {
      try {
        await deleteNotification(notificationId)
        
        // 从列表中移除
        const index = this.messages[type].findIndex(
          msg => msg.notificationId === notificationId
        )
        if (index !== -1) {
          this.messages[type].splice(index, 1)
        }
        
        return true
      } catch (error) {
        console.error('删除通知失败:', error)
        throw error
      }
    },

    /**
     * 标记为已读
     */
    async markMessageAsRead(notificationId) {
      try {
        await markAsRead(notificationId)
        // TODO: 更新本地未读计数
      } catch (error) {
        console.error('标记已读失败:', error)
        throw error
      }
    },

    /**
     * 全部标记为已读
     */
    async markAllAsRead(type = this.currentTab) {
      try {
        await markAllAsRead(type)
        
        // 更新本地未读计数
        this.unreadCounts[type] = 0
        this.updateTotalUnread()
      } catch (error) {
        console.error('批量标记已读失败:', error)
        throw error
      }
    },

    /**
     * 获取未读数量
     */
    async fetchUnreadCount() {
      try {
        const response = await getUnreadCount()
        if (response.code === 200 && response.data) {
          this.unreadCounts = {
            ...this.unreadCounts,
            ...response.data,
            total: Object.values(response.data).reduce((sum, count) => sum + count, 0)
          }
        }
      } catch (error) {
        console.error('获取未读数量失败:', error)
      }
    },

    /**
     * 更新总未读数
     */
    updateTotalUnread() {
      this.unreadCounts.total = Object.values(this.unreadCounts).reduce(
        (sum, count) => sum + count,
        0
      )
    },

    /**
     * 获取消息设置
     */
    async fetchSettings() {
      try {
        const response = await getMessageSettings()
        if (response.code === 200 && response.data) {
          this.settings = { ...this.settings, ...response.data }
        }
      } catch (error) {
        console.error('获取消息设置失败:', error)
        throw error
      }
    },

    /**
     * 更新消息设置
     */
    async updateSettings(newSettings) {
      try {
        await updateMessageSettings(newSettings)
        this.settings = { ...this.settings, ...newSettings }
        return true
      } catch (error) {
        console.error('更新消息设置失败:', error)
        throw error
      }
    },

    /**
     * 清空指定类型的消息缓存
     */
    clearCache(type) {
      if (type) {
        this.messages[type] = []
        this.pagination[type] = { page: 1, size: 20, total: 0, hasMore: true }
      } else {
        // 清空所有
        Object.keys(this.messages).forEach(key => {
          this.messages[key] = []
          this.pagination[key] = { page: 1, size: 20, total: 0, hasMore: true }
        })
      }
    },

    /**
     * 刷新当前消息
     */
    async refreshCurrentMessages() {
      await this.loadMessages(this.currentTab, true)
    }
  }
})
