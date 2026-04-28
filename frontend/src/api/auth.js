import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export function checkAuth() {
  return request({
    url: '/auth/check',
    method: 'get'
  })
}

export function getCurrentUserFullProfile() {
  return request({
    url: '/profile/me',
    method: 'get'
  })
}

export function updateAvatar(data) {
  return request({
    url: '/profile/avatar',
    method: 'post',
    data
  })
}
