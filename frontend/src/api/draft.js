import request from '@/utils/request'

export function getDraftList() {
  return request({
    url: '/drafts/list',
    method: 'get'
  })
}

export function getDraftDetail(id) {
  return request({
    url: `/drafts/${id}`,
    method: 'get'
  })
}

export function saveDraft(data) {
  return request({
    url: '/drafts/save',
    method: 'post',
    data
  })
}

export function deleteDraft(id) {
  return request({
    url: `/drafts/${id}`,
    method: 'delete'
  })
}

export function batchDeleteDrafts(data) {
  return request({
    url: '/drafts/batch',
    method: 'delete',
    data
  })
}
