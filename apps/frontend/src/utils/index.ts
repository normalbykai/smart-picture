import {saveAs} from "file-saver";
import {message} from "ant-design-vue";

/**
 * 格式化文件大小
 * @param size
 */
export const formatSize = (size?: number) => {
  if (!size) {
    return ' 未知'
  }

  if (size < 1024) return size + ' B'

  if (size < (1024 * 1024)) return (size / 1024).toFixed(2) + ' KB'

  // if (size < (1024 * 1024 * 1024)) return (size / 1024 / 1024).toFixed(2) + ' MB'
  return (size / 1024 / 1024).toFixed(2) + ' MB'
}


/**
 * 下载图片通过 url地址
 *
 * @param url
 * @param fileName
 */
export const downloadImage = (url: string, fileName?: string) => {
  if (!url) {
    return;
  }
  saveAs(url, fileName)
}

/**
 * 转换为16进制的颜色代码 如 0x000000 转换为 #000000
 *
 * @param color 后端返回的主色调，可能为空
 * @returns 标准 #RRGGBB 颜色，无法解析时返回空字符串
 */
export const toHexColor = (color?: string): string => {
  if (!color) {
    return '';
  }

  // 去掉前缀0x
  const colorValue = color.startsWith('0x') ? color.slice(2) : color

  // 将剩余部分转换为十六进制数，在转换为6位 十六进制数
  const value = parseInt(colorValue, 16).toString(16).padStart(6, '0')

  // 返回标准的颜色格式 #RRGGBB
  return `#${value}`;
}

/**
 * 文件上传校验（供上传组件在 beforeUpload 中调用）
 *
 * @param file 待上传文件
 * @returns true 表示校验通过
 */
export const uploadValid = (file: any): boolean => {
    let fileTYpe = file.type
    let isJpegOrPng = fileTYpe === 'image/jpeg' || fileTYpe === 'image/png'
    // 1. 格式正确
    if (!isJpegOrPng) {
      message.error('不支持该文件上传，推荐使用JPEG 或 PNG格式')
      return false
    }

    // 2. 图片小于2M
    const isLessThan2M = file.size / 1024 / 1024 < 2
    if (!isLessThan2M) {
      message.error('不能上传超过 2M 大小的图片')
      return false
    }

    // 都满足校验才返回ture
    return isJpegOrPng && isLessThan2M
}
