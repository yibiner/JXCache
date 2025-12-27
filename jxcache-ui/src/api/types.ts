// 通用类型
export interface PageRequest {
  pageNo: number
  pageSize: number
}

export interface PageResult<T> {
  items: T[]
  total: number
  pageNo: number
  pageSize: number
}

// 观察接口类型
export interface QueryRequest {
  area: string
  cacheName: string
  level?: string
  keyPrefix?: string
  pageRequest?: PageRequest
  shard?: number
  totalShards?: number
}

export interface CacheEntry {
  key: string
  valuePreview: string
  valueType: string
  createTime: number
  lastAccessTime: number
  expireTime: number
}

export interface LocalCacheSnapshot {
  nodeId: string
  area: string
  cacheName: string
  entries: CacheEntry[]
  total: number
  partial: boolean
  queryTime: number
  level?: string
  message?: string
}

export interface LocalCacheEntryDetail {
  nodeId: string
  area: string
  cacheName: string
  key: string
  value: string
  valueType: string
  valueLength: number
  truncated: boolean
  queryTime: number
  requestedLevel?: string
  hitLevel?: string
}

export interface InvalidateRequest {
  area: string
  cacheName: string
  key?: string
  invalidateRemote?: boolean
}

export interface InvalidateResult {
  success: boolean
  nodeId: string
  area: string
  cacheName: string
  key?: string
  localInvalidated: boolean
  remoteInvalidated: boolean
  errorMessage?: string
  operationTime: number
}

// 聚合接口类型
export interface AggregateResult {
  results: LocalCacheSnapshot[]
  failedNodes: string[]
  partial: boolean
  totalTimeMs: number
}

export interface ConsistencyResult {
  consistent: boolean
  nodeCount: number
  valueCount: number
  nodes: Array<{
    nodeId: string
    value: string | null
    exists: boolean
  }>
  queryTime: number
}

export interface ServiceInstance {
  nodeId: string
  host: string
  port: number
  healthy: boolean
  metadata?: Record<string, string>
}

