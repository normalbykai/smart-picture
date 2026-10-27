import 'axios'

/**
 * 扩展 axios 的请求配置。
 *
 * 项目的 OpenAPI 生成代码会传入 `requestType`（源自 umi-request），
 * axios 本身会忽略未知字段，但 TypeScript 需要显式声明才能通过类型检查。
 */
declare module 'axios' {
  export interface AxiosRequestConfig {
    requestType?: 'form' | 'json' | string
  }
}
