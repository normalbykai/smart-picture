import request from '@/utils/request'

export interface PictureAnalysisResult {
  title: string
  tags: string[]
}

/** AI 分析仅返回建议，确认后的保存仍走图片更新接口。 */
export function analyzePicture(pictureId: number | string) {
  return request<{ code: number; data?: PictureAnalysisResult; message?: string }>(
    '/picture/analyze',
    {
      method: 'POST',
      data: { pictureId },
      // 模型生成比普通接口耗时更长，覆盖默认请求超时。
      timeout: 110_000,
    },
  )
}
