import { createRequest } from './request'
import type {
  QueryRequest,
  AggregateResult,
  ConsistencyResult,
  ServiceInstance,
  InvalidateRequest,
  InvalidateResult
} from './types'

/**
 * 创建 Aggregator API
 * @param baseUrl 服务器地址
 */
export function createAggregatorApi(baseUrl: string) {
  const request = createRequest(baseUrl)
  
  return {
    // 获取服务节点列表
    getNodes: (serviceName: string): Promise<ServiceInstance[]> => {
      return request.get(`/api/jxc/aggregate/nodes?serviceName=${encodeURIComponent(serviceName)}`)
    },

    // 聚合查询
    query: (serviceName: string, params: QueryRequest, targets?: string[]): Promise<AggregateResult> => {
      let url = `/api/jxc/aggregate/query?serviceName=${encodeURIComponent(serviceName)}`
      if (targets && targets.length > 0) {
        targets.forEach(target => {
          url += `&targets=${encodeURIComponent(target)}`
        })
      }
      return request.post(url, params)
    },

    // 获取单个条目（所有节点）
    getEntry: (serviceName: string, area: string, name: string, key: string, targets?: string[]): Promise<any[]> => {
      let url = `/api/jxc/aggregate/entry?serviceName=${encodeURIComponent(serviceName)}&area=${encodeURIComponent(area)}&name=${encodeURIComponent(name)}&key=${encodeURIComponent(key)}`
      if (targets && targets.length > 0) {
        targets.forEach(target => {
          url += `&targets=${encodeURIComponent(target)}`
        })
      }
      return request.get(url)
    },

    // 一致性检查
    checkConsistency: (serviceName: string, area: string, name: string, key: string, targets?: string[]): Promise<ConsistencyResult> => {
      let url = `/api/jxc/aggregate/entry/consistency?serviceName=${encodeURIComponent(serviceName)}&area=${encodeURIComponent(area)}&name=${encodeURIComponent(name)}&key=${encodeURIComponent(key)}`
      if (targets && targets.length > 0) {
        targets.forEach(target => {
          url += `&targets=${encodeURIComponent(target)}`
        })
      }
      return request.get(url)
    },

    // 聚合失效
    invalidate: (serviceName: string, params: InvalidateRequest, targets?: string[]): Promise<InvalidateResult[]> => {
      if (params.key) {
        let url = `/api/jxc/aggregate/invalidate?serviceName=${encodeURIComponent(serviceName)}&area=${encodeURIComponent(params.area)}&name=${encodeURIComponent(params.cacheName)}&key=${encodeURIComponent(params.key)}&invalidateRemote=${params.invalidateRemote || false}`
        if (targets && targets.length > 0) {
          targets.forEach(target => {
            url += `&targets=${encodeURIComponent(target)}`
          })
        }
        return request.delete(url)
      } else {
        let url = `/api/jxc/aggregate/invalidate?serviceName=${encodeURIComponent(serviceName)}`
        if (targets && targets.length > 0) {
          targets.forEach(target => {
            url += `&targets=${encodeURIComponent(target)}`
          })
        }
        return request.post(url, params)
      }
    }
  }
}
