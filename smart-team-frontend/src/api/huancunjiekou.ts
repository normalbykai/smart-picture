// @ts-ignore
/* eslint-disable */
import request from '@/utils/request.ts'

/** 清空所有缓存 清空所有缓存 POST /admin/cache/clear/all */
export async function clearCacheAll(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/admin/cache/clear/all', {
    method: 'POST',
    ...(options || {}),
  })
}

/** 清空主页的缓存记录 清空主页的缓存记录 POST /admin/cache/clear/home */
export async function clearCacheHome(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/admin/cache/clear/home', {
    method: 'POST',
    ...(options || {}),
  })
}
