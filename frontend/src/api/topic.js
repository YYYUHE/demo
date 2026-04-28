import request from '@/utils/request'

export function getPopularTopics(params) {
  return request({
    url: '/topics/popular',
    method: 'get',
    params
  })
}

export function getTopicSuggestions(params) {
  return request({
    url: '/topics/suggestions',
    method: 'get',
    params
  })
}

export function getOrCreateTopic(data) {
  return request({
    url: '/topics/get-or-create',
    method: 'post',
    data
  })
}

export function incrementTopicUsage(topicName) {
  return request({
    url: `/topics/${encodeURIComponent(topicName)}/increment-usage`,
    method: 'post'
  })
}
