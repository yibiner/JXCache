import { createRequest } from './request'
import type {
  QueryRequest,
  LocalCacheSnapshot,
  LocalCacheEntryDetail,
  InvalidateRequest,
  InvalidateResult
} from './types'

/**
 * 创建 Observer API
 * @param baseUrl 服务器地址
 */
export function createObserverApi(baseUrl: string) {
  const request = createRequest(baseUrl)
  
  return {
    // 获取缓存区域列表
    getAreas: (): Promise<string[]> => {
      return request.get('/api/jxc/observer/areas')
    },

    // 查询缓存数据
    query: (params: QueryRequest): Promise<LocalCacheSnapshot> => {
      return request.post('/api/jxc/observer/query', params)
    },

    // 获取单个缓存条目详情
    getEntry: (area: string, name: string, key: string, level: string = 'AUTO'): Promise<LocalCacheEntryDetail> => {
      return request.get(`/api/jxc/observer/entry?area=${encodeURIComponent(area)}&name=${encodeURIComponent(name)}&key=${encodeURIComponent(key)}&level=${encodeURIComponent(level)}`)
    },

    // 失效缓存
    invalidate: (params: InvalidateRequest): Promise<InvalidateResult> => {
      if (params.key) {
        return request.delete(
          `/api/jxc/observer/invalidate?area=${encodeURIComponent(params.area)}&name=${encodeURIComponent(params.cacheName)}&key=${encodeURIComponent(params.key)}&invalidateRemote=${params.invalidateRemote || false}`
        )
      } else {
        return request.post('/api/jxc/observer/invalidate', params)
      }
    }
  }
}
