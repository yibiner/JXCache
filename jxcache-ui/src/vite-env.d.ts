/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_OBSERVER_URL: string
  readonly VITE_AGGREGATOR_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module 'element-plus/dist/locale/zh-cn.mjs' {
  const zhCn: any
  export default zhCn
}

declare module 'element-plus/dist/locale/en.mjs' {
  const en: any
  export default en
}
