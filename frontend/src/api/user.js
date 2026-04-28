import request from '@/utils/request'

export function getUsersBasic(ids = []) {
  return request({
    url: '/users/basic',
    method: 'get',
    params: { ids: ids.join(',') }
  })
}

