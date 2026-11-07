# papa-ui · 纺织行业多模态智能体前端

Vue 3 + Vite + Element Plus 实现的对话工作台，配套后端 [`../src`](../src/main/java/com/zoe/java/ai/langchain4j)。

## 页面

| 路由 | 页面 | 说明 |
|---|---|---|
| `/login` | 登录 | 图形验证码 + JWT，登录态存本地，已登录自动跳转 `/chat` |
| `/chat` | 对话工作台 | SSE 流式对话、历史会话侧栏、图片理解 / 文生图、语音输入、文件问答、停止生成 |
| `/rag` | 知识库管理 | 文档上传（单 / 批量）、列表、删除、处理日志 |

## 目录

```
src/
├── api/            # login.js（认证）、rag.js（知识库）
├── components/
│   ├── login/      # 登录页
│   ├── chatwindow/ # 对话主界面
│   ├── rag/        # 知识库管理
│   └── layout/     # 整体布局与导航
├── router/         # 路由与登录守卫
├── store/          # 用户态
├── utils/          # 请求封装、token 读写、环境变量
└── constants/      # 常量
```

## 开发

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # 产物在 dist/
```

开发环境通过 Vite 代理联调后端：`/api/**` 去掉 `/api` 前缀后转发到 `http://localhost:8082`
（见 `vite.config.js` 的 `server.proxy`）。**后端端口变更时这里要同步改。**

站点标题由 `.env.development` 的 `VITE_APP_TITLE` 控制。
