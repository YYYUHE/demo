import request from '@/utils/request'

export function getPostList(params) {
  return request({
    url: '/posts/list',
    method: 'get',
    params
  })
}

export function getPostDetail(id) {
  return request({
    url: `/posts/${id}`,
    method: 'get'
  })
}

export function publishPost(data) {
  return request({
    url: '/posts/publish',
    method: 'post',
    data
  })
}

export function deletePost(id) {
  return request({
    url: `/posts/${id}`,
    method: 'delete'
  })
}

export function likePost(id) {
  return request({
    url: `/posts/${id}/like`,
    method: 'post'
  })
}

export function unlikePost(id) {
  return request({
    url: `/posts/${id}/unlike`,
    method: 'post'
  })
}

export function favoritePost(id) {
  return request({
    url: `/posts/${id}/favorite`,
    method: 'post'
  })
}

export function unfavoritePost(id) {
  return request({
    url: `/posts/${id}/unfavorite`,
    method: 'post'
  })
}

export function getFavoritePosts(params) {
  return request({
    url: '/posts/favorites',
    method: 'get',
    params
  })
}
