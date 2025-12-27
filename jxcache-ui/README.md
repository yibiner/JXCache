# JXCache UI

JXCache UI 是 JXCache 的前端管理界面，面向缓存排查、跨节点对比和运维辅助场景。它通过标准 REST API 连接 Observer 或 Aggregator 服务，把本地缓存快照、单 key 详情、多节点聚合结果和一致性检查以可操作的 Web 页面呈现出来。

这个子项目更像一个“缓存治理控制台”的前端原型：轻量、独立部署、可连接多个后端地址，适合在演示环境、测试环境或内部平台中快速集成。

## 已实现能力

- **Observer 工作台**：连接单个 Observer 服务，查询 L0 本地缓存快照，支持 area、cacheName、keyPrefix、分页和 level 参数。
- **缓存详情与失效**：查看单个 key 的完整值、命中层级和类型信息，并支持指定 key 失效。
- **Aggregator 工作台**：连接 Aggregator 服务，加载服务节点，按目标节点聚合查询缓存数据。
- **一致性检查**：对多个节点上的同一 key 做值对比，辅助定位多 Pod 缓存不一致问题。
- **双服务地址配置**：Observer 与 Aggregator 可以分别配置后端地址，并保存在浏览器 localStorage。
- **主题与国际化**：支持暗色/亮色主题切换，内置中文和英文语言包。

## 技术栈


| 类型   | 技术                      |
| ---- | ----------------------- |
| 框架   | Vue 3 + Composition API |
| 语言   | TypeScript              |
| 构建   | Vite                    |
| UI   | Element Plus + SCSS     |
| 路由   | Vue Router              |
| 状态基础 | Pinia                   |
| 国际化  | Vue I18n                |
| HTTP | Axios                   |


说明：`Pinia` 已作为应用基础能力挂载，当前页面状态主要由组件内状态管理；图表类能力可在后续指标面板中继续扩展。

## 快速开始

### 环境要求

- Node.js 16+
- npm 7+，或兼容的 pnpm/yarn

### 安装与启动

```bash
cd jxcache-ui
npm install
npm run dev
```

默认访问：

```text
http://localhost:5173
```

### 配置后端地址

UI 默认使用 `http://localhost:8080`。你可以在页面顶部输入后端地址，也可以创建 `.env.development` 或 `.env.production`：

```bash
VITE_OBSERVER_URL=http://localhost:18081
VITE_AGGREGATOR_URL=http://localhost:19001
```

如果通过 Vite dev server 代理后端接口，请按你的本地环境调整 `vite.config.ts` 中的 `server.proxy`，不要把内部测试环境地址作为通用开源默认值。

### 构建与预览

```bash
npm run build
npm run preview
```

构建产物位于 `dist/`。

## 页面说明

### Observer 页面

典型流程：

1. 输入 Observer 服务地址，例如 `http://localhost:18081`。
2. 输入 `area` 和 `cacheName`，例如 `default`、`dropletCacheById`。
3. 可选输入 `keyPrefix`，用于缩小扫描范围。
4. 使用 `L0` 查询列表；如需 L1/AUTO，请通过单 key 详情查询。
5. 在结果表格中查看 key、valuePreview、valueType 和创建时间。
6. 点击详情查看完整值，或点击失效执行指定 key 删除。

### Aggregator 页面

典型流程：

1. 输入 Aggregator 服务地址，例如 `http://localhost:19001`。
2. 输入服务名，例如 `water` 或 `ocean`。
3. 加载节点列表，可选择部分节点作为 targets。
4. 输入 `area`、`cacheName`，可选输入具体 key。
5. 执行聚合查询、一致性检查或跨节点失效。
6. 通过节点 tab 查看每个节点返回的缓存数据。

## 项目结构

```text
jxcache-ui
├── src
│   ├── api              # Axios 封装、Observer/Aggregator API、类型定义
│   ├── i18n             # 中文/英文语言包和 i18n 初始化
│   ├── layouts          # 主布局、导航、主题与语言切换
│   ├── router           # /observer 与 /aggregator 路由
│   ├── styles           # 全局样式和主题变量
│   ├── utils            # 日期、主题、API 地址配置
│   ├── views            # Observer 与 Aggregator 页面
│   ├── App.vue
│   └── main.ts
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 部署建议

### 静态站点部署

```bash
npm run build
```

将 `dist/` 部署到 Nginx、对象存储静态站点或内部前端平台，并把 `/api` 或指定域名反向代理到 JXCache 后端服务。

Nginx 示例：

```nginx
server {
    listen 80;
    server_name jxcache.example.com;

    root /opt/jxcache-ui/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://jxcache-backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 与后端集成部署

也可以把 `dist/` 产物放入后端服务的静态资源目录，由后端统一托管。这种方式适合内部工具，但需要后端处理前端路由 fallback。

## 开发约定

- 页面使用 Vue 3 Composition API 和 TypeScript。
- UI 组件优先使用 Element Plus，复杂样式放在 SCSS 中维护。
- API 类型统一维护在 `src/api/types.ts`。
- Observer 与 Aggregator 的后端地址分开存储，避免单节点排查和聚合排查互相覆盖。
- 新增页面时同步补充中英文语言包。

## Roadmap

- 增加节点拓扑、服务健康、失败节点聚合视图。
- 增加缓存值 diff、JSON 格式化和大对象折叠展示。
- 增加操作审计、危险操作确认、只读模式和角色化菜单。
- 增加指标面板，展示查询耗时、失败率、缓存命中和失效操作趋势。
- 补充端到端测试与截图回归，提升开源演示稳定性。

## License

JXCache UI 跟随 JXCache 主项目使用 Apache License 2.0。