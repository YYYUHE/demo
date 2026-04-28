import request from '@/utils/request'

export function createComment(postId, data) {
  return request({
    url: `/posts/${postId}/comments`,
    method: 'post',
    data
  })
}

export function getComments(postId, params) {
  return request({
    url: `/posts/${postId}/comments`,
    method: 'get',
    params
  })
}

export function likeComment(id) {
  return request({
    url: `/comments/${id}/like`,
    method: 'post'
  })
}
