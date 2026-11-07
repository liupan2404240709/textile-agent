# RAG 增强检索系统部署指南

## 功能概述

本系统实现了完整的 RAG（Retrieval-Augmented Generation）增强检索功能，包括：

1. **文档上传**：支持 PDF、TXT、Markdown、Word 格式文档
2. **文档解析**：自动解析不同格式的文档内容
3. **文档切分**：智能将长文档切分为合适的文本片段
4. **向量化**：使用阿里云 Qwen Embedding 模型生成向量
5. **索引存储**：将向量数据存储到 Milvus 向量数据库
6. **智能检索**：聊天时自动从知识库中检索相关内容

## 前置要求

### 1. Milvus 向量数据库

需要安装并启动 Milvus 服务：

```bash
# 使用 Docker 快速启动 Milvus
docker run -d --name milvus-standalone \
  -p 19530:19530 \
  -p 9091:9091 \
  milvusdb/milvus:v2.3.0 \
  milvus run standalone
```

或者参考 [Milvus 官方文档](https://milvus.io/docs/install_standalone-docker.md) 进行安装。

### 2. 阿里云 DashScope API Key

确保在 `application.yml` 中配置了有效的 API Key：

```yaml
rag:
  embedding:
    api-key: your-dashscope-api-key
```

## 后端配置

### 1. Maven 依赖

已在 `pom.xml` 中添加 Milvus 支持：

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-milvus</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

### 2. 应用配置

在 `application.yml` 中配置 RAG 相关参数：

```yaml
rag:
  # Milvus 向量数据库配置
  milvus:
    host: localhost
    port: 19530
    collectionName: textile_knowledge
  # 本地文档存储路径
  local:
    storage:
      path: ./documents/rag_uploads
  # Embedding API Key
  embedding:
    api-key: ${DASHSCOPE_API_KEY}
```

### 3. 核心组件

#### MilvusConfig.java
- 创建 Milvus Embedding Store Bean
- 管理本地文档存储路径
- 提供文档加载功能

#### RagDocumentService.java
- 文档上传和保存
- 文档解析（PDF、TXT、MD、DOC/DOCX）
- 文档切分（递归字符切分器）
- 向量化处理（Qwen Embedding）
- 批量上传支持

#### RagDocumentController.java
REST API 接口：
- `POST /api/rag/upload` - 上传单个文档
- `POST /api/rag/upload-batch` - 批量上传文档
- `GET /api/rag/documents` - 获取文档列表
- `DELETE /api/rag/document/{fileName}` - 删除文档

## 前端使用

### 1. 访问 RAG 管理页面

启动应用后，在左侧边栏点击 "RAG 文档管理" 菜单项即可进入。

### 2. 上传文档

1. 拖拽文件或点击选择文件
2. 支持多选，可同时上传多个文档
3. 点击 "开始上传" 按钮
4. 等待处理完成（大文件可能需要几分钟）

### 3. 查看和管理文档

- 查看所有已上传的文档列表
- 删除不需要的文档

### 4. 在聊天中使用 RAG

上传文档后，在聊天窗口提问时，系统会自动：
1. 从 Milvus 中检索与问题相关的文档片段
2. 将检索到的内容作为上下文提供给大模型
3. 大模型基于检索到的知识生成回答

## API 使用示例

### 上传单个文档

```javascript
import { uploadDocument } from '@/api/rag'

const file = document.querySelector('input[type="file"]').files[0]
const result = await uploadDocument(file)
console.log(result.data) // "文档上传成功: xxx.pdf, 生成 25 个文本片段"
```

### 批量上传

```javascript
import { uploadBatchDocuments } from '@/api/rag'

const files = Array.from(document.querySelector('input[type="file"]').files)
const result = await uploadBatchDocuments(files)
console.log(result.data) // "批量处理完成: 成功 5/5 个文件, 共生成 120 个文本片段"
```

### 获取文档列表

```javascript
import { getDocuments } from '@/api/rag'

const response = await getDocuments()
console.log(response.data) // ["doc_20250101_120000_abc123.pdf", ...]
```

### 删除文档

```javascript
import { deleteDocument } from '@/api/rag'

await deleteDocument('doc_20250101_120000_abc123.pdf')
```

## 技术细节

### 文档处理流程

```
用户上传文档
    ↓
保存到本地文件夹 (./documents/rag_uploads)
    ↓
解析文档内容 (根据文件格式选择解析器)
    ↓
切分文档 (递归字符切分，每段 500 字符，重叠 50 字符)
    ↓
向量化 (Qwen text-embedding-v2 模型，1536 维)
    ↓
存储到 Milvus 向量数据库
```

### 文档切分策略

- **最大片段长度**：500 字符
- **重叠长度**：50 字符
- **切分方式**：递归字符切分（优先在段落、句子边界切分）

### 向量模型

- **模型**：阿里云 Qwen text-embedding-v2
- **维度**：1536
- **API**：DashScope

### Milvus 集合配置

- **集合名称**：textile_knowledge
- **向量维度**：1536
- **相似度度量**：余弦相似度（默认）

#### ⚠️ 集合改名后的重建操作

业务域由工业设计域切换为纺织行业域后，集合名由 `industrial_design_knowledge` 变更为 `textile_knowledge`。
旧集合中的向量数据与新集合互相独立，**不会自动迁移**，请按以下步骤重建：

1. 确认 `application.yml` 中的配置为新集合名：
   ```yaml
   rag:
     milvus:
       collectionName: textile_knowledge
   ```
2. 清理 `documents/rag_uploads/` 下的旧领域文档（工业设计相关），放入纺织行业领域文档
   （纺织材料、纱线织造、印染后整理、花型图案、检测标准、可持续纺织等；
   已提供种子文档 `纺织行业基础知识.txt`）
3. 通过 RAG 文档管理页面（前端 `/rag`）或 `RagDocumentController` 接口重新上传文档，
   系统会使用新集合名自动创建集合并写入向量
4. 若需要释放旧集合占用的资源，可手动删除：
   ```bash
   # 使用 Milvus 管理工具或 MilvusCollectionResetter 删除旧集合 industrial_design_knowledge
   ```
5. 验证：上传完成后执行查询，确认能检索到新文档内容；若检索为空，检查集合名与文档是否匹配

## 注意事项

1. **Milvus 服务**：确保 Milvus 服务已启动并可访问
2. **文件大小限制**：单个文件不超过 50MB
3. **超时设置**：大文件上传和向量化可能需要较长时间，API 超时已设置为 5 分钟
4. **向量数据清理**：目前删除文档仅删除本地文件，Milvus 中的向量数据需要手动清理
5. **API Key 安全**：请妥善保管 DashScope API Key，不要提交到版本控制系统
6. **集合名一致性**：`application.yml`、`MilvusConfig`、`MilvusDataChecker`、`MilvusCollectionResetter`
   中的集合名必须保持一致，否则会出现"上传成功但检索不到"的静默降级问题

## 故障排查

### 1. Milvus 连接失败

检查 Milvus 服务是否正常运行：
```bash
docker ps | grep milvus
```

检查端口是否可访问：
```bash
telnet localhost 19530
```

### 2. 文档上传失败

- 检查文件大小是否超过限制
- 检查文件格式是否支持
- 查看后端日志确认具体错误信息

### 3. 向量化失败

- 检查 DashScope API Key 是否有效
- 检查网络连接是否正常
- 查看 API 配额是否充足

### 4. 检索效果不佳

- 调整文档切分参数（片段大小、重叠长度）
- 调整检索参数（minScore、maxResults）
- 增加更多高质量的领域文档

## 后续优化方向

1. **向量数据管理**：实现 Milvus 中文档的完整生命周期管理
2. **多集合支持**：为不同领域创建不同的向量集合
3. **混合检索**：结合关键词检索和向量检索
4. **重排序**：对检索结果进行二次排序
5. **缓存机制**：缓存常见问题的检索结果
6. **监控统计**：记录文档处理状态、检索命中率等指标

## 联系与支持

如有问题或建议，请联系开发团队。
