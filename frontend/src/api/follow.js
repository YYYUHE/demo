import request from '@/utils/request'

export function getMyFollowing() {
  return request({
    url: '/follow/my-following',
    method: 'get'
  })
}

export function toggleFollow(userId) {
  return request({
    url: `/follow/${userId}/toggle`,
    method: 'post'
  })
}

export function checkFollowStatus(userId) {
  return request({
    url: `/follow/${userId}/check`,
    method: 'get'
  })
}
