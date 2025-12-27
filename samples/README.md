# JXCache Samples

`samples` 是 JXCache 的场景化演示目录，用三个递进服务展示从单节点缓存观测到跨节点缓存治理的完整链路。它的价值不只是“能跑起来”，而是把 JetCache 在微服务多实例场景下最常见的排查问题压缩成可复现样例：哪个节点有缓存、同一个 key 在多个节点是否一致、失效操作是否传播到目标节点。

## 示例拓扑

```text
samples
├── water   # Observer only，3 个 LOCAL 缓存节点，端口 18081/18082/18083
├── river   # Observer + Aggregator，2 个 LOCAL 缓存节点，端口 19001/19002
└── ocean   # Observer + Aggregator + Nacos + Redis，3 个 BOTH 缓存节点，端口 20001/20002/20003
```

## 场景定位

| 示例 | 缓存类型 | JXCache 能力 | 适合验证 |
| --- | --- | --- | --- |
| `water` | `CacheType.LOCAL` | Observer | 本地缓存扫描、单 key 详情、指定 key 失效 |
| `river` | `CacheType.LOCAL` | Observer + Aggregator | 通过 Fixed/Nacos 发现 `water` 节点并聚合查询 |
| `ocean` | `CacheType.BOTH` | Observer + Aggregator | L0/L1 查询、多节点一致性、Redis 远端缓存场景 |

`water` 是最轻量、最推荐先跑通的示例。`river` 和 `ocean` 默认带有 Nacos/Redis 相关配置，直接运行前请先按本机或测试环境替换配置；如果只想本地演示聚合查询，可以关闭 Nacos 并使用 Fixed 注册中心。

## 快速开始

### 1. 安装 JXCache 本地依赖

示例工程没有纳入根 POM 聚合构建，运行前请先在仓库根目录安装 JXCache 模块：

```bash
mvn clean install
```

### 2. 启动 `water` 多节点

```bash
bash samples/water/src/main/script/run-water-nodes.sh
```

脚本会启动 3 个节点：

- `water-node1`: `http://localhost:18081`
- `water-node2`: `http://localhost:18082`
- `water-node3`: `http://localhost:18083`

生成一些缓存数据：

```bash
curl http://localhost:18081/api/water/1
curl http://localhost:18082/api/water/2
curl http://localhost:18083/api/water/3
```

查询单节点 Observer：

```bash
curl http://localhost:18081/api/jxc/observer/areas

curl -X POST "http://localhost:18081/api/jxc/observer/query" \
  -H "Content-Type: application/json" \
  -d '{
    "area": "default",
    "cacheName": "dropletCacheById",
    "pageRequest": {
      "pageNo": 1,
      "pageSize": 20
    }
  }'
```

### 3. 启动 `river` 聚合查询

```bash
bash samples/river/src/main/script/run-river-nodes.sh
```

`river` 可以作为聚合端查询 `water` 节点：

```bash
curl "http://localhost:19001/api/jxc/aggregate/nodes?serviceName=water"

curl -X POST "http://localhost:19001/api/jxc/aggregate/query?serviceName=water" \
  -H "Content-Type: application/json" \
  -d '{
    "area": "default",
    "cacheName": "dropletCacheById",
    "pageRequest": {
      "pageNo": 1,
      "pageSize": 20
    }
  }'
```

### 4. 启动 `ocean` BOTH 缓存示例

`ocean` 演示本地缓存 + Redis 远端缓存的 BOTH 模式，并依赖 Nacos 服务发现。运行前请检查：

- Nacos 地址、用户名、密码、分组是否已替换为你的环境。
- Redis 集群或单机配置是否可访问。
- 端口 `20001`、`20002`、`20003` 未被占用。

```bash
bash samples/ocean/src/main/script/run-ocean-nodes.sh
```

生成缓存并检查一致性：

```bash
curl http://localhost:20001/api/ocean/1
curl http://localhost:20002/api/ocean/1
curl http://localhost:20003/api/ocean/1

curl "http://localhost:20001/api/jxc/aggregate/entry/consistency?serviceName=ocean&area=default&name=oceanCacheById&key=1"
```

## 服务接口速览

### Water API

- `GET /api/water/{dropletId}`：按 ID 查询水滴，写入 `dropletCacheById`。
- `GET /api/water/name/{dropletName}`：按名称查询，写入 `water` area 的 `dropletCacheByName`。
- `GET /api/water/color/{color}`：按颜色查询列表，写入 `water` area 的 `dropletCacheByColor`。
- `PUT /api/water/update`：通过 JetCache 注解更新指定 ID 缓存。
- `DELETE /api/water/{dropletId}`：通过 JetCache 注解失效指定 ID 缓存。

### River API

- `GET /api/river/{riverId}`：按 ID 查询河流，写入 `riverCacheById`。
- `GET /api/river/name/{riverName}`：按名称查询，写入 `river` area 的 `riverCacheByName`。
- `GET /api/river/flow/{flowDirection}`：按流向查询列表。
- `GET /api/jxc/aggregate/*`：作为聚合端查询其他服务节点。

### Ocean API

- `GET /api/ocean/{oceanId}`：按 ID 查询海洋，写入 BOTH 类型的 `oceanCacheById`。
- `GET /api/ocean/name/{oceanName}`：按名称查询，写入 `ocean` area 的 `oceanCacheByName`。
- `GET /api/ocean/location/{location}`：按位置查询列表。
- `GET /api/jxc/aggregate/entry/consistency`：检查多节点同 key 缓存值一致性。

## 关键配置

### Observer

```yaml
jxc:
  observer:
    enabled: true
    maxPageSize: 200
    maxValuePreview: 200
    totalShards: 8
```

### Aggregator

```yaml
jxc:
  aggregator:
    enabled: true
    perNodeTimeoutMs: 2000
    totalTimeoutMs: 4000
    maxConcurrency: 16
    observerScanPath: /api/jxc/observer/query
    observerEntryPath: /api/jxc/observer/entry
    observerInvalidatePath: /api/jxc/observer/invalidate
```

### Fixed 注册中心

```yaml
jxc:
  registry:
    fixed:
      enabled: true
      priority: 1000
      services:
        - name: water
          nodes:
            - nodeId: droplet-1
              host: 127.0.0.1
              port: 18081
              healthy: true
            - nodeId: droplet-2
              host: 127.0.0.1
              port: 18082
              healthy: true
```

## 排查建议

- 服务启动失败时，先检查端口占用和 Maven 本地仓库中是否已有 `dev.yibin:jxcache-*` 依赖。
- `river` 或 `ocean` 启动失败时，优先检查 Nacos/Redis 配置；这些示例保留了测试环境配置结构，开源运行时应替换为本机环境。
- 聚合查询为空时，确认目标服务名是否与 `spring.application.name` 或 Fixed 配置中的 `name` 一致。
- Observer 查询为空时，先调用业务接口生成缓存，再查询对应的 `area` 和 `cacheName`。
- BOTH 缓存场景下，列表扫描只适合 L0 本地缓存；远端 L1 更适合通过单 key 查询验证。

## 与根项目的关系

这些示例用于展示 JXCache 的接入方式和排查路径，不是生产模板。生产落地时建议把 Nacos/Redis 地址、鉴权策略、日志级别、端口、部署脚本和缓存 TTL 按团队标准重新治理。
