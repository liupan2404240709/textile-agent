# Excel 生成功能使用指南

## 📊 功能概述

系统现已支持通过 AI 对话生成真正的 Excel 文件（.xlsx格式），用户可以请求创建各种数据表格。

---

## ✅ 已完成的功能

### 1. **ExcelGenerationTools 工具类**
- 位置：`src/main/java/com/zoe/java/ai/langchain4j/tools/ExcelGenerationTools.java`
- 功能：根据提供的数据生成 Excel 文件
- 输出目录：`./generated_files/`

### 2. **集成到 PapaAgent**
- 已将 Excel 生成工具注册到 ToolProvider
- AI 可以自动调用该工具响应用户的 Excel 生成请求

### 3. **支持的图表类型**
- ✅ 柱状图、条形图、折线图、饼图等（通过 AntV Chart MCP）
- ✅ 真正的 Excel 文件（通过 ExcelGenerationTools）

---

## 🚀 使用方法

### 方式一：直接描述需求

**示例对话：**
```
用户：生成一份关于毛衣不同价格对比的excel表格

AI：（自动调用 generate_excel 工具）
✅ Excel 文件已生成，路径：./generated_files/excel_1714139511332.xlsx
```

### 方式二：提供详细数据

**示例对话：**
```
用户：请帮我创建一个销售数据表格，包含以下信息：
产品名称 | 单价 | 销量 | 总金额
毛衣A   | 199  | 100  | 19900
毛衣B   | 299  | 80   | 23920
毛衣C   | 399  | 60   | 23940

AI：（自动生成 Excel 文件并返回路径）
```

### 方式三：结合 AntV Chart 可视化

**示例对话：**
```
用户：先生成毛衣价格对比的Excel表格，然后用柱状图展示

AI：
1. 调用 generate_excel 生成表格
2. 调用 AntV Chart MCP 生成柱状图
3. 返回两个文件的访问路径
```

---

## 📝 技术细节

### ExcelGenerationTools 工具参数

```java
@Tool(name = "generate_excel", value = "根据提供的数据生成Excel文件")
public String generateExcel(
    String title,              // 表格标题
    List<String> headers,      // 表头列表
    List<List<String>> data    // 数据行
)
```

### 生成的文件特性

- ✅ 自动调整列宽
- ✅ 表头加粗显示
- ✅ 灰色背景突出表头
- ✅ 边框线条清晰
- ✅ 数字自动识别为数值类型
- ✅ 文件名包含时间戳，避免冲突

### 文件存储

- **目录**：`./generated_files/`
- **命名规则**：`excel_{timestamp}.xlsx`
- **格式**：Office Open XML (.xlsx)

---

## 💡 实际应用示例

### 场景1：产品价格对比

```
用户：创建一个毛衣价格对比表，包含品牌、材质、价格三个字段

AI 生成：
| 品牌   | 材质     | 价格 |
|--------|----------|------|
| 恒源祥 | 羊毛     | 399  |
| 海澜之家 | 羊绒混纺 | 599  |
| 优衣库 | 棉混纺   | 199  |
```

### 场景2：销售数据分析

```
用户：生成Q1销售报表，包含月份、产品类别、销售额、增长率

AI 生成：
| 月份 | 产品类别 | 销售额(万) | 增长率 |
|------|----------|------------|--------|
| 1月  | 毛衣     | 120        | 15%    |
| 2月  | 毛衣     | 95         | -20%   |
| 3月  | 毛衣     | 110        | 16%    |
```

### 场景3：库存管理

```
用户：创建一个库存清单，包含SKU、品名、库存数量、预警状态

AI 生成：
| SKU    | 品名       | 库存数量 | 预警状态 |
|--------|------------|----------|----------|
| SW001  | 羊毛衫-红  | 150      | 正常     |
| SW002  | 羊绒衫-黑  | 20       | 低库存   |
| SW003  | 针织衫-白  | 300      | 充足     |
```

---

## 🔧 配置说明

### application.yml 配置

```yaml
mcp:
  antv-chart:
    enabled: true  # 启用 AntV 图表服务（可选）
```

### Maven 依赖

项目已包含 Apache POI 依赖（用于 Excel 生成）：
```xml
<!--解析Word文档（doc/docx）-->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-document-parser-apache-poi</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

---

## ⚠️ 注意事项

1. **文件访问**：生成的文件存储在服务器本地，需要确保有读取权限
2. **文件大小**：建议单次生成不超过 10000 行数据
3. **中文支持**：完全支持中文内容
4. **数字识别**：系统会自动识别数字类型的单元格
5. **重启应用**：修改配置后需要重启 Spring Boot 应用

---

## 🎯 与 AntV Chart 的区别

| 特性 | ExcelGenerationTools | AntV Chart MCP |
|------|---------------------|----------------|
| 输出格式 | .xlsx 文件 | 图片 URL |
| 可编辑性 | ✅ 可在 Excel 中编辑 | ❌ 仅图片 |
| 数据处理 | ✅ 支持公式、筛选 | ❌ 静态图表 |
| 适用场景 | 数据分析、报表 | 可视化展示 |
| 文件大小 | 几 KB - 几 MB | 几十 KB |

---

## 📌 最佳实践

1. **明确数据结构**：提供清晰的表头和数据
2. **合理的数据量**：避免生成过大的表格
3. **结合使用**：Excel + 图表组合效果更佳
4. **定期清理**：定期清理 `./generated_files/` 目录中的旧文件

---

## 🔍 故障排查

### 问题1：AI 没有调用 Excel 生成工具

**原因**：应用未重启或配置未生效

**解决方案**：
1. 检查 `ExcelGenerationTools` 是否被正确扫描
2. 重启 Spring Boot 应用
3. 查看启动日志确认工具注册成功

### 问题2：生成的文件找不到

**原因**：路径错误或权限问题

**解决方案**：
```bash
# 检查目录是否存在
ls -la ./generated_files/

# 如果不存在，手动创建
mkdir -p ./generated_files
```

### 问题3：中文乱码

**原因**：编码问题（理论上不会发生，POI 默认 UTF-8）

**解决方案**：
- 确保数据库和文件系统使用 UTF-8 编码
- 检查操作系统区域设置

---

## 📞 技术支持

如有问题，请查看：
1. 应用启动日志（确认工具注册）
2. 运行时日志（查看工具调用情况）
3. `./generated_files/` 目录（确认文件生成）

---

**更新时间**：2026-04-26  
**版本**：v1.0
