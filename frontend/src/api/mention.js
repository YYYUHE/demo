import request from '@/utils/request'

/**
 * 搜索关注的用户（用于@功能）
 */
export function searchFollowingUsers(keyword) {
  return request({
    url: '/mentions/search',
    method: 'get',
    params: { keyword }
  })
}

/**
 * 获取@我的列表
 */
export function getMyMentions(page = 1, size = 20) {
  return request({
    url: '/mentions/me',
    method: 'get',
    params: { page, size }
  })
}

/**
 * 标记@通知为已读
 */
export function markMentionAsRead(id) {
  return request({
    url: `/mentions/${id}/read`,
    method: 'post'
  })
}

/**
 * 删除@通知
 */
export function deleteMention(id) {
  return request({
    url: `/mentions/${id}`,
    method: 'delete'
  })
}

/**
 * 获取未读@数量
 */
export function getUnreadMentionCount() {
  return request({
    url: '/mentions/unread-count',
    method: 'get'
  })
}
