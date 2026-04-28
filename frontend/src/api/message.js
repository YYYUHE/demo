import request from '@/utils/request'

/**
 * 获取回复我的消息
 */
export function getReplyMessages(params = {}) {
  return request({
    url: '/messages/replies',
    method: 'get',
    params: {
      page: params.page || 1,
      size: params.size || 20
    }
  })
}

/**
 * 获取点赞我的消息
 */
export function getLikeMessages(params = {}) {
  return request({
    url: '/messages/likes',
    method: 'get',
    params: {
      page: params.page || 1,
      size: params.size || 20
    }
  })
}

/**
 * 获取@我的消息
 */
export function getMentionMessages(params = {}) {
  return request({
    url: '/messages/mentions',
    method: 'get',
    params: {
      page: params.page || 1,
      size: params.size || 20
    }
  })
}

/**
 * 获取收到的赞消息
 */
export function getLikesReceivedMessages(params = {}) {
  return request({
    url: '/messages/likes-received',
    method: 'get',
    params: {
      page: params.page || 1,
      size: params.size || 20
    }
  })
}

/**
 * 获取系统通知
 */
export function getSystemMessages(params = {}) {
  return request({
    url: '/messages/system',
    method: 'get',
    params: {
      page: params.page || 1,
      size: params.size || 20
    }
  })
}

/**
 * 删除通知
 */
export function deleteNotification(notificationId) {
  return request({
    url: `/messages/replies/${notificationId}`,
    method: 'delete'
  })
}

/**
 * 标记消息为已读
 */
export function markAsRead(notificationId) {
  return request({
    url: `/messages/${notificationId}/read`,
    method: 'put'
  })
}

/**
 * 批量标记为已读
 */
export function markAllAsRead(type) {
  return request({
    url: '/messages/read-all',
    method: 'put',
    params: { type }
  })
}

/**
 * 获取未读消息数量
 */
export function getUnreadCount() {
  return request({
    url: '/messages/unread-count',
    method: 'get'
  })
}

/**
 * 获取消息设置
 */
export function getMessageSettings() {
  return request({
    url: '/messages/settings',
    method: 'get'
  })
}

/**
 * 更新消息设置
 */
export function updateMessageSettings(settings) {
  return request({
    url: '/messages/settings',
    method: 'put',
    data: settings
  })
}
