# AntV MCP 图表服务器集成指南

## 📊 概述

已成功集成 AntV MCP 图表服务器，支持通过大模型生成 15+ 种类型的图表。

## ✅ 已完成的配置

### 1. 配置文件更新

**application.yml** 中添加了配置项：
```yaml
mcp:
  antv-chart:
    enabled: true  # 是否启用 AntV 图表 MCP 服务
```

### 2. Java 配置类更新

**McpConfig.java** 中添加了：
- `antvChartTransport()` - AntV Chart MCP 传输层
- `antvChartClient()` - AntV Chart MCP 客户端

## 🎨 支持的图表类型

AntV MCP 服务器支持以下 15+ 种图表：

1. **generate_area_chart** - 面积图
2. **generate_bar_chart** - 条形图
3. **generate_column_chart** - 柱状图
4. **generate_dual_axes_chart** - 双轴图
5. **generate_fishbone_diagram** - 鱼骨图
6. **generate_flow_diagram** - 流程图
7. **generate_histogram_chart** - 直方图
8. **generate_line_chart** - 折线图
9. **generate_mind_map** - 思维导图
10. **generate_network_graph** - 网络图
11. **generate_pie_chart** - 饼图
12. **generate_radar_chart** - 雷达图
13. **generate_scatter_chart** - 散点图
14. **generate_treemap_chart** - 树图
15. **generate_word_cloud_chart** - 词云图

## 🚀 使用方法

### 方式一：通过对话自动调用

用户直接在聊天中描述需要生成的图表，AI 会自动调用相应的 MCP 工具。

**示例对话：**
```
用户：请帮我生成一个销售数据的柱状图，数据如下：
- 一月：100万
- 二月：150万
- 三月：120万
- 四月：180万

AI：（自动调用 generate_column_chart 工具）
返回图表 URL
```

### 方式二：指定图表类型

**示例对话：**
```
用户：用折线图展示最近一周的温度变化
- 周一：25°C
- 周二：28°C
- 周三：30°C
- 周四：27°C
- 周五：26°C
- 周六：29°C
- 周日：31°C

AI：（自动调用 generate_line_chart 工具）
返回图表 URL
```

## ⚙️ 启动流程

1. **启动应用时**，系统会自动：
   - 检查 `mcp.antv-chart.enabled` 配置
   - 如果启用，通过 `npx -y @antv/mcp-server-chart` 启动 MCP 服务
   - 创建 MCP 客户端并注册到 ToolProvider

2. **首次启动注意**：
   - 首次运行可能需要下载 npm 包，请耐心等待
   - 确保系统已安装 Node.js 和 npx 命令
   - 查看控制台日志确认启动状态

## 🔍 日志输出

启动时会看到类似日志：
```
📊 [MCP配置] 正在启动 AntV Chart MCP 服务: cmd.exe /c npx -y @antv/mcp-server-chart
ℹ️  提示：首次启动可能需要下载 npm 包，请耐心等待...
✅ 正在创建 AntV Chart MCP 客户端
✅ MCP ToolProvider 初始化完成，共加载 X 个 MCP 客户端
  - MCP Client: DefaultMcpClient
```

## ❌ 故障排查

### 问题1：Node.js 未安装
**错误信息**：`启动 AntV Chart MCP 服务失败，请确认已安装 Node.js 和 npx 命令`

**解决方案**：
1. 下载并安装 Node.js：https://nodejs.org/
2. 验证安装：
   ```bash
   node --version
   npx --version
   ```

### 问题2：npm 包下载失败
**错误信息**：网络超时或下载失败

**解决方案**：
1. 检查网络连接
2. 配置 npm 镜像源：
   ```bash
   npm config set registry https://registry.npmmirror.com
   ```
3. 手动预安装：
   ```bash
   npx -y @antv/mcp-server-chart
   ```

### 问题3：不想使用 AntV Chart 功能
**解决方案**：
在 `application.yml` 中禁用：
```yaml
mcp:
  antv-chart:
    enabled: false
```

## 💡 最佳实践

1. **数据结构清晰**：提供清晰的键值对数据，便于 AI 理解
2. **明确图表类型**：如果需要特定图表，直接说明（如"用饼图展示"）
3. **包含标题**：建议提供图表标题，使图表更完整
4. **合理的数据量**：避免过多数据点影响图表可读性

## 📝 示例场景

### 场景1：业务数据分析
```
请生成一个柱状图展示各产品线的季度销售额：
- 产品线A：Q1=100万, Q2=120万, Q3=130万, Q4=150万
- 产品线B：Q1=80万, Q2=90万, Q3=110万, Q4=125万
- 产品线C：Q1=60万, Q2=75万, Q3=85万, Q4=95万
```

### 场景2：趋势分析
```
用折线图展示用户增长趋势：
- 1月：1000人
- 2月：1500人
- 3月：2200人
- 4月：3000人
- 5月：4200人
- 6月：5500人
```

### 场景3：占比分析
```
生成饼图展示市场份额分布：
- 公司A：35%
- 公司B：25%
- 公司C：20%
- 公司D：12%
- 其他：8%
```

### 场景4：关系展示
```
创建一个思维导图，主题是"项目管理"：
- 计划阶段
  - 需求分析
  - 资源分配
- 执行阶段
  - 任务分解
  - 进度跟踪
- 收尾阶段
  - 验收测试
  - 文档归档
```

## 🔧 技术细节

- **连接方式**：stdio（标准输入输出）
- **启动命令**：`cmd.exe /c npx -y @antv/mcp-server-chart`
- **许可证**：遵循 AntV 开源协议
- **NPM 包**：`@antv/mcp-server-chart`

## 📌 注意事项

1. AntV Chart MCP 服务会在应用启动时自动启动
2. 每次图表生成会返回一个图像 URL
3. 图表渲染由 AntV 服务端完成，无需本地渲染
4. 确保网络可以访问 AntV 图表服务

---

**集成完成时间**：2026-04-26  
**版本**：v1.0
