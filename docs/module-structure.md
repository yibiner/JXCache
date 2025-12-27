# 模块结构说明

## 项目架构

JXCache 采用模块化设计，遵循分层架构原则，确保各模块职责清晰、依赖关系合理。

## 模块依赖关系图

```
jxcache (父聚合模块)
├── jxcache-common (基础模块)
│   ├── Spring Boot 启动器
│   └── FastJSON2
├── jxcache-registry-spi (注册中心SPI)
│   └── jxcache-common
├── jxcache-observer (观察者模块)
│   ├── jxcache-common
│   └── JetCache
├── jxcache-registry-fixed (固定注册中心)
│   ├── jxcache-common
│   └── jxcache-registry-spi
├── jxcache-registry-nacos (Nacos注册中心)
│   ├── jxcache-common
│   └── jxcache-registry-spi
├── jxcache-aggregator-core (聚合核心)
│   ├── jxcache-common
│   └── jxcache-registry-spi
├── jxcache-starter-observer (观察模块启动器)
│   ├── jxcache-common
│   └── jxcache-observer
├── jxcache-starter-aggregator-core (聚合启动器 - 固定注册中心)
│   ├── jxcache-common
│   ├── jxcache-registry-spi
│   ├── jxcache-aggregator-core
│   └── jxcache-registry-fixed (可选)
├── jxcache-starter-aggregator-nacos (聚合 + Nacos 启动器)
│   ├── jxcache-starter-aggregator-core
│   ├── jxcache-registry-fixed (降级方案)
│   └── jxcache-registry-nacos (主要注册中心)
└── jxcache-tests (测试模块)
    ├── jxcache-starter-observer
    ├── jxcache-starter-aggregator-core
    └── jxcache-registry-fixed
```

## 模块详细说明

### 基础层

#### jxcache-common
**职责**：提供基础 DTO、SPI 接口和默认实现
- **核心类**：
  - `PageRequest`、`PageResult` - 分页相关
  - `CacheEntry`、`LocalCacheSnapshot` - 缓存数据模型
  - `QueryRequest`、`AggregateResult` - 查询相关
  - `ServiceInstance` - 服务实例模型
- **SPI 接口**：
  - `ValuePreviewer` - 值预览器
  - `AccessGuard` - 访问守卫
  - `RateLimiter` - 限流器
  - `RegistryClient` - 注册中心客户端
- **依赖**：无内部依赖

### SPI 抽象层

#### jxcache-registry-spi
**职责**：提供注册中心 SPI 抽象
- **核心类**：
  - `RegistryClient` - 注册中心客户端 SPI 接口
  - `RegistryClientFactory` - 注册中心客户端工厂（支持优先级和降级策略）
- **依赖**：jxcache-common

### 核心功能层

#### jxcache-observer
**职责**：提供本地缓存观察功能
- **核心类**：
  - `LocalCacheIntrospector` - 本地缓存内省器接口（支持插件化扩展）
  - `AbstractLocalCacheIntrospector` - 抽象基类（提供通用实现）
  - `CaffeineLocalCacheIntrospector` - Caffeine 缓存内省器实现
  - `LinkedHashMapLocalCacheIntrospector` - LinkedHashMap 缓存内省器实现
  - `LocalCacheIntrospectorFactory` - 内省器工厂（自动选择实现）
  - `LocalCacheService` - 本地缓存服务（封装查询逻辑）
  - `ObserverController` - 观察接口控制器
  - `LocalCacheEntryDetail` - 单键详情数据模型（支持完整数据输出）
- **功能**：
  - 本地缓存只读扫描（支持多种缓存类型）
  - 分页、前缀过滤、分片查询
  - 单个缓存键的完整数据查询（`/api/jxc/observer/entry`）
  - REST 接口
  - 插件化扩展：支持新增缓存类型，无需修改现有代码
- **依赖**：jxcache-common

#### jxcache-registry-fixed
**职责**：提供固定注册中心实现
- **核心类**：
  - `StaticRegistryProperties` - 配置属性
  - `StaticRegistryClient` - 固定注册中心客户端
- **功能**：
  - 基于配置文件的静态服务发现
  - 支持多节点配置
  - 优先级较低（默认1000），通常作为降级方案
- **依赖**：jxcache-common, jxcache-registry-spi

#### jxcache-registry-nacos
**职责**：提供 Nacos 注册中心实现
- **核心类**：
  - `NacosRegistryProperties` - 配置属性
  - `NacosRegistryClient` - Nacos 注册中心客户端
- **功能**：
  - 基于 Nacos 服务发现获取服务实例
  - 支持服务白名单配置
  - 优先级较高（默认100），优先使用
- **依赖**：jxcache-common, jxcache-registry-spi, spring-cloud-starter-alibaba-nacos-discovery

#### jxcache-aggregator-core
**职责**：提供聚合查询核心功能
- **核心类**：
  - `AggregateQueryService` - 聚合查询服务（支持批量查询和单个条目查询）
  - `AggregateController` - 聚合查询控制器
  - `AggregatorConfig` - 聚合器配置（RestTemplate）
- **功能**：
  - 跨节点聚合查询缓存快照
  - 跨节点聚合查询单个缓存条目
  - 并发查询和失败降级
  - 超时控制和容错处理
- **依赖**：jxcache-common, jxcache-registry-spi

### 集成层

#### jxcache-starter-observer
**职责**：提供观察模块自动配置
- **核心类**：
  - `ObserverProperties` - 配置属性
  - `ObserverAutoConfiguration` - 自动配置类
- **功能**：
  - Spring Boot 自动配置支持
  - 配置属性绑定
  - 依赖管理
- **依赖**：jxcache-common, jxcache-observer

#### jxcache-starter-aggregator-core
**职责**：提供聚合模块自动配置（固定注册中心）
- **核心类**：
  - `AggregatorProperties` - 配置属性
  - `AggregatorAutoConfiguration` - 自动配置类
- **功能**：
  - Spring Boot 自动配置支持
  - 配置属性绑定
  - 自动扫描 registry-spi 和 aggregator 包
  - 默认使用固定注册中心（需手动引入 `registry-fixed`）
- **依赖**：jxcache-common, jxcache-registry-spi, jxcache-aggregator-core

#### jxcache-starter-aggregator-nacos
**职责**：提供聚合+Nacos 组合自动配置（支持降级）
- **核心类**：
  - `AggregatorNacosAutoConfiguration` - 自动配置类
- **功能**：
  - 聚合查询 + Nacos 注册中心（优先级高）
  - 聚合查询 + 固定注册中心（优先级低，作为降级方案）
  - 自动扫描 `registry-nacos`、`registry-fixed` 和 `registry-spi` 包
  - 当 Nacos 不可用时自动降级到固定配置
- **依赖**：jxcache-starter-aggregator-core, jxcache-registry-fixed, jxcache-registry-nacos

### 测试层

#### jxcache-tests
**职责**：提供集成测试和端到端测试
- **测试类**：
  - `FixedRegistrySmokeIT` - 固定注册中心冒烟测试
- **功能**：
  - 跨模块集成测试
  - 端到端功能测试
  - 性能测试
- **依赖**：多个启动器模块

## 编译顺序

### 正确的编译顺序

1. **jxcache-common** (基础模块)
2. **jxcache-registry-spi** (SPI抽象)
3. **jxcache-observer** (观察者核心)
4. **jxcache-registry-fixed** (固定注册中心)
5. **jxcache-registry-nacos** (Nacos注册中心)
6. **jxcache-aggregator-core** (聚合核心)
7. **jxcache-starter-observer** (观察模块启动器)
8. **jxcache-starter-aggregator-core** (聚合模块启动器)
9. **jxcache-starter-aggregator-nacos** (聚合 + Nacos 启动器)
10. **jxcache-tests** (测试模块)

### Maven 自动构建

Maven 会根据依赖关系自动确定构建顺序，无需手动指定。

## 模块职责划分

### 基础模块
- **jxcache-common**：提供通用 DTO、SPI 接口和默认实现

### 核心功能模块
- **jxcache-observer**：本地缓存观察功能
- **jxcache-aggregator-core**：聚合查询核心功能
- **jxcache-registry-spi**：注册中心 SPI 抽象
- **jxcache-registry-fixed**：固定注册中心实现
- **jxcache-registry-nacos**：Nacos 注册中心实现

### 集成模块
- **jxcache-starter-***：提供 Spring Boot 自动配置

### 测试模块
- **jxcache-tests**：集成测试和端到端测试

## 最佳实践

### 1. 开发顺序
1. 先开发基础模块（common）
2. 再开发核心功能模块
3. 最后开发集成和测试模块

### 2. 测试策略
1. 单元测试：每个模块独立测试
2. 集成测试：跨模块功能测试
3. 端到端测试：完整业务流程测试

### 3. 版本管理
1. 使用语义化版本号
2. 破坏性变更时升级主版本号
3. 向后兼容的新功能升级次版本号
4. 修复 bug 升级修订版本号

## 注意事项

1. **循环依赖**：避免模块间的循环依赖
2. **依赖最小化**：每个模块只依赖必要的其他模块
3. **接口隔离**：通过 SPI 接口实现模块间的松耦合
4. **测试隔离**：每个模块的测试应该独立运行
5. **构建优化**：合理使用 Maven 的并行构建功能

