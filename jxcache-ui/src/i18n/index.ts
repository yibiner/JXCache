import { createI18n } from 'vue-i18n'
import zhCn from './locales/zh-cn.json'
import en from './locales/en.json'

export type Locale = 'zh-cn' | 'en'

const messages = {
  'zh-cn': zhCn,
  'en': en
}

/**
 * 检测浏览器语言
 * 如果是中文相关语言，返回 'zh-cn'，否则返回 'en'
 */
function detectBrowserLocale(): Locale {
  const browserLang = navigator.language || (navigator as any).userLanguage || 'en'
  
  // 检查是否是中文相关语言
  if (browserLang.toLowerCase().startsWith('zh')) {
    return 'zh-cn'
  }
  
  // 非中文情况下默认显示英文
  return 'en'
}

export function setupI18n() {
  // 优先使用保存的语言设置，如果没有则根据浏览器语言自动适配
  const savedLocale = localStorage.getItem('locale') as Locale | null
  const locale = savedLocale || detectBrowserLocale()
  
  const i18n = createI18n({
    legacy: false,
    locale: locale,
    fallbackLocale: 'en', // 默认回退到英文
    messages
  })

  return i18n
}

export function setLocale(locale: Locale) {
  localStorage.setItem('locale', locale)
  window.location.reload()
}

