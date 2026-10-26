/// <reference types="vite/client" />

/**
 * 通用 .vue 模块声明。
 *
 * vue-cropper 1.1.4 的类型入口内部直接 import 了未声明类型的 .vue 文件，
 * 在严格模式下会报 TS7016。加上标准的 .vue 模块声明后即可正常解析。
 */
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, any>
  export default component
}

/**
 * vue-cropper 模块声明（覆盖包内不完整的类型入口）。
 */
declare module 'vue-cropper' {
  import type { DefineComponent } from 'vue'

  export const VueCropper: DefineComponent<Record<string, unknown>, Record<string, unknown>, any>
  export const globalCropper: {
    version: string
    install: (app: import('vue').App) => void
    VueCropper: DefineComponent<Record<string, unknown>, Record<string, unknown>, any>
  }
  const defaultExport: typeof globalCropper
  export default defaultExport
}
