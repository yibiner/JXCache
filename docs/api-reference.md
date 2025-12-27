# API 参考文档

JXCache 暴露了一组只读 REST 接口，用于在运行中的 JetCache 节点上检查本地 Caffeine 缓存，并在必要时跨节点聚合查询数据。

## 基础约定

- 基础 REST 前缀：`/api/jxc`
- 内容类型：`application/json`
- 字符编码：`UTF-8`

## Observer API（外部访问）

### POST `/api/jxc/observer/query`

批量浏览指定 `area + cacheName` 下的本地缓存键，支持键前缀过滤、分页和分片。

**请求体 (`QueryRequest`)**
```json
{
  "area": "iot-service",
  "cacheName": "getDeviceBySn",
  "keyPrefix": "123",
  "pageRequest": {
    "pageNumber": 1,
    "pageSize": 20
  },
  "shard": 0,
  "totalShards": 1
}
```

**响应体 (`LocalCacheSnapshot`)**
```json
{
  "nodeId": "local",
  "area": "iot-service",
  "cacheName": "getDeviceBySn",
  "entries": [
    {
      "key": "12345678",
      "valuePreview": "{\\"sn\\":\\"12345678\\",...}",
      "valueType": "DeviceDTO",
      "createTime": 1698739200000,
      "lastAccessTime": 1698742800000,
      "expireTime": 0
    }
  ],
  "total": 42,
  "partial": false,
  "queryTime": 1698746400123
}
```

### GET `/api/jxc/observer/entry`

获取单个缓存键的完整值。结果不会被截断，也无需分页。

| 参数 | 位置 | 说明 |
| ---- | ---- | ---- |
| `area` | query | JetCache `area` |
| `name` | query | JetCache `cacheName` |
| `key` | query | 缓存键（按字符串匹配） |

**响应体 (`LocalCacheEntryDetail`)**
```json
{
  "nodeId": "local",
  "area": "iot-service",
  "cacheName": "getDeviceBySn",
  "key": "12345678",
  "value": "{\\"sn\\":\\"12345678\\",\\"model\\":\\"Pro\\"}",
  "valueType": "DeviceDTO",
  "valueLength": 68,
  "truncated": false,
  "queryTime": 1698746400456
}
```

未命中时返回 `404 NOT_FOUND`。

### DELETE `/api/jxc/observer/invalidate`

失效本地缓存条目。

| 参数 | 位置 | 说明 |
| ---- | ---- | ---- |
| `area` | query | JetCache `area` |
| `name` | query | JetCache `cacheName` |
| `key` | query | 缓存键（可选，如果为空则不支持） |
| `invalidateRemote` | query | 是否同时失效远程缓存（Redis），默认 false |

**响应体 (`InvalidateResult`)**
```json
{
  "success": true,
  "nodeId": "local",
  "area": "iot-service",
  "cacheName": "getDeviceBySn",
  "key": "12345678",
  "localInvalidated": true,
  "remoteInvalidated": false,
  "errorMessage": null,
  "operationTime": 1698746400456
}
```

### POST `/api/jxc/observer/invalidate`

失效本地缓存条目（POST方式，支持复杂请求体）。

**请求体 (`InvalidateRequest`)**
```json
{
  "area": "iot-service",
  "cacheName": "getDeviceBySn",
  "key": "12345678",
  "invalidateRemote": false
}
```

**响应体 (`InvalidateResult`)**：同上。

## Aggregator API

### POST `/api/jxc/aggregate/query`

对注册中心返回的节点并发调用 `/api/jxc/observer/query` 接口并聚合结果。

| 参数 | 位置 | 说明 |
| ---- | ---- | ---- |
| `serviceName` | query | 注册中心中的服务名 |
| `targets` | query (可选，可重复) | 指定仅聚合的节点 ID |

请求体与 `QueryRequest` 相同。

**响应体 (`AggregateResult`)**
```json
{
  "results": [
    {
      "nodeId": "node-a",
      "area": "iot-service",
      "cacheName": "getDeviceBySn",
      "entries": [...],
      "total": 21,
      "partial": false,
      "queryTime": 1698746400123
    }
  ],
  "failedNodes": [],
  "partial": false,
  "totalTimeMs": 318
}
```

### GET `/api/jxc/aggregate/nodes`

返回注册中心中目标服务的节点清单（依赖 `RegistryClient` 的实现，固定注册中心会返回配置节点；Nacos 注册中心会返回注册的服务实例；如未配置，可能得到空列表）。

### GET `/api/jxc/aggregate/entry`

聚合查询多个节点的单个缓存条目。返回所有节点中该键的完整值。

| 参数 | 位置 | 说明 |
| ---- | ---- | ---- |
| `serviceName` | query | 注册中心中的服务名 |
| `area` | query | JetCache `area` |
| `name` | query | JetCache `cacheName` |
| `key` | query | 缓存键（按字符串匹配） |
| `targets` | query (可选，可重复) | 指定仅聚合的节点 ID |

**响应体**：`List<LocalCacheEntryDetail>`

```json
[
  {
    "nodeId": "node-a",
    "area": "iot-service",
    "cacheName": "getDeviceBySn",
    "key": "12345678",
    "value": "{\"sn\":\"12345678\",\"model\":\"Pro\"}",
    "valueType": "DeviceDTO",
    "valueLength": 68,
    "truncated": false,
    "queryTime": 1698746400456
  },
  {
    "nodeId": "node-b",
    "area": "iot-service",
    "cacheName": "getDeviceBySn",
    "key": "12345678",
    "value": "{\"sn\":\"12345678\",\"model\":\"Pro\"}",
    "valueType": "DeviceDTO",
    "valueLength": 68,
    "truncated": false,
    "queryTime": 1698746400456
  }
]
```

## 核心数据模型速览

### `QueryRequest`

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `area` | string | JetCache 区域 |
| `cacheName` | string | JetCache 缓存名称 |
| `keyPrefix` | string? | 键前缀过滤器 |
| `pageRequest` | `PageRequest`? | 分页参数（页码从 1 开始） |
| `shard` | int | 当前分片索引（默认 0） |
| `totalShards` | int | 分片总数（默认 1） |

### `LocalCacheSnapshot`

- `nodeId` / `area` / `cacheName`
- `entries`: `CacheEntry` 列表
- `total`: 实际返回条目数（考虑分页 & 分片）
- `partial`: 是否仅返回局部结果
- `queryTime`: 采样时间（毫秒）

### `CacheEntry`

- `key`: 字符串化的键
- `valuePreview`: 经过 `ValuePreviewer` 截断后的字符串
- `valueType`: 值类型简单类名
- `createTime` / `lastAccessTime` / `expireTime`: 取样时间戳（若 JetCache 无法提供真实统计则为当前时间）

### `LocalCacheEntryDetail`

- `key`, `value`, `valueType`
- `valueLength`: 字符串长度，便于前端估算展示
- `truncated`: 单键查询固定为 `false`
- `queryTime`: 查询时间戳

### `AggregateResult`

- `results`: 每个成功节点返回的 `LocalCacheSnapshot`
- `failedNodes`: 拉取失败的节点 ID 列表
- `partial`: 是否存在失败节点
- `totalTimeMs`: 聚合耗时

## 常见错误

| HTTP 状态 | 场景 |
| --------- | ---- |
| `400` | 请求缺少 `area`/`cacheName` 等必要字段 |
| `404` | 单键查询未命中缓存 |
| `429` | 自定义 `RateLimiter` 拒绝请求 |
| `500` | 内部错误或注册中心不可用 |
| `504` | 聚合调用在超时内未完成 |

## 集成建议

- 生产环境下建议实现 `AccessGuard` 和 `RateLimiter` SPI，加入鉴权与限流。
- 对敏感数据可自定义 `ValuePreviewer`，在批量接口中继续使用预览，在单键调试时再查看完整值。
- 通过 `/api/jxc/observer/areas/recompute` 可在缓存配置刷新后手动同步区域清单。

## 示例调用

```bash
# 批量浏览缓存
curl -X POST "http://localhost:18081/api/jxc/observer/query" \
  -H "Content-Type: application/json" \
  -d '{
        "area":"iot-service",
        "cacheName":"getDeviceBySn",
        "keyPrefix":"12345"
      }'

# 精确读取缓存键
curl "http://localhost:18081/api/jxc/observer/entry?area=iot-service&name=getDeviceBySn&key=12345678"

# 发起聚合查询
curl -X POST "http://localhost:18090/api/jxc/aggregate/query?serviceName=demo-app" \
  -H "Content-Type: application/json" \
  -d '{"area":"iot-service","cacheName":"getDeviceBySn"}'

# 聚合查询单个缓存条目
curl "http://localhost:18090/api/jxc/aggregate/entry?serviceName=demo-app&area=iot-service&name=getDeviceBySn&key=12345678"
```
