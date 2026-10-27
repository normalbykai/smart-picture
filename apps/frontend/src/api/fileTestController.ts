// @ts-ignore
/* eslint-disable */
import request from '@/utils/request.ts'

/** 此处后端没有提供注释 POST /file/test/file/upload */
export async function testFile(body: {}, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/file/test/file/upload', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
