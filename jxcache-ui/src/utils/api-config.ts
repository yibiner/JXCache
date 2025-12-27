/**
 * API 配置工具
 * 支持 Observer 和 Aggregator 独立配置服务器地址
 */

const OBSERVER_URL_KEY = 'jxc_observer_url'
const AGGREGATOR_URL_KEY = 'jxc_aggregator_url'

const DEFAULT_URL = 'http://localhost:8080'

/**
 * 获取 Observer 服务地址
 */
export function getObserverUrl(): string {
  const saved = localStorage.getItem(OBSERVER_URL_KEY)
  if (saved) return saved
  if (import.meta.env.VITE_OBSERVER_URL) return import.meta.env.VITE_OBSERVER_URL
  return DEFAULT_URL
}

/**
 * 设置 Observer 服务地址
 */
export function setObserverUrl(url: string): void {
  try {
    new URL(url)
    localStorage.setItem(OBSERVER_URL_KEY, url)
  } catch {
    throw new Error('Invalid URL format')
  }
}

/**
 * 获取 Aggregator 服务地址
 */
export function getAggregatorUrl(): string {
  const saved = localStorage.getItem(AGGREGATOR_URL_KEY)
  if (saved) return saved
  if (import.meta.env.VITE_AGGREGATOR_URL) return import.meta.env.VITE_AGGREGATOR_URL
  return DEFAULT_URL
}

/**
 * 设置 Aggregator 服务地址
 */
export function setAggregatorUrl(url: string): void {
  try {
    new URL(url)
    localStorage.setItem(AGGREGATOR_URL_KEY, url)
  } catch {
    throw new Error('Invalid URL format')
  }
}
