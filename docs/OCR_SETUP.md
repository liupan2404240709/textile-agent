# OCR 环境安装指南

## 概述
本项目已集成 Tesseract OCR，可以处理扫描件PDF和图片型文档。

## 安装步骤

### 1. 安装 Tesseract OCR 引擎

#### Windows 系统
1. 下载 Tesseract 安装程序：
   - 官方GitHub: https://github.com/UB-Mannheim/tesseract/wiki
   - 推荐版本: tesseract-w64-setup-5.x.x.exe

2. 运行安装程序，记住安装路径（例如：`C:\Program Files\Tesseract-OCR`）

3. 将 Tesseract 添加到系统 PATH 环境变量：
   ```
   C:\Program Files\Tesseract-OCR
   ```

#### macOS 系统
```bash
brew install tesseract
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt-get update
sudo apt-get install tesseract-ocr
sudo apt-get install libtesseract-dev
```

### 2. 下载 OCR 语言包

项目默认配置支持**中文简体**和**英文**识别。

#### 方法一：从官方仓库下载（推荐）
1. 访问: https://github.com/tesseract-ocr/tessdata
2. 下载以下文件：
   - `chi_sim.traineddata` (中文简体)
   - `eng.traineddata` (英文)

#### 方法二：使用命令行下载
```bash
# 在项目根目录创建 tessdata 文件夹
mkdir tessdata
cd tessdata

# 下载中文简体语言包
curl -L -o chi_sim.traineddata https://github.com/tesseract-ocr/tessdata/raw/main/chi_sim.traineddata

# 下载英文语言包
curl -L -o eng.traineddata https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata
```

### 3. 放置语言包文件

将下载的 `.traineddata` 文件放到项目根目录的 `tessdata` 文件夹：

```
E:\ai_project\ChatLLM-medical-main-papa\
├── tessdata\
│   ├── chi_sim.traineddata    # 中文简体
│   └── eng.traineddata        # 英文
├── src\
├── pom.xml
└── ...
```

### 4. 验证安装

#### 检查 Tesseract 是否安装成功
```bash
tesseract --version
```

应该看到类似输出：
```
tesseract 5.x.x
 leptonica-1.x.x
```

#### 测试 OCR 功能
启动 Spring Boot 应用后，查看日志中是否有：
```
OCR 环境检查通过
```

如果看到警告信息，请根据提示检查：
- TessData 目录是否存在
- 语言包文件是否正确放置

## 配置说明

在 `application.yml` 中可以调整 OCR 配置：

```yaml
rag:
  ocr:
    enabled: true                    # 启用/禁用 OCR
    auto-fallback: true              # 自动回退到 OCR
    language: chi_sim+eng            # 识别语言
    data-path: ./tessdata            # 语言包路径
    dpi: 300                         # PDF转图片DPI
```

### 可选语言包
- `chi_sim` - 中文简体
- `chi_tra` - 中文繁体
- `eng` - 英文
- `jpn` - 日文
- `kor` - 韩文

多语言用 `+` 连接，例如：`chi_sim+eng+jpn`

### DPI 设置建议
- **200**: 快速但可能影响识别精度
- **300**: 平衡速度和精度（推荐）
- **400-600**: 高精度但速度慢，内存占用高

## 性能优化建议

1. **扫描件质量**
   - 确保扫描清晰，文字可读
   - 避免倾斜、模糊、阴影
   - 分辨率建议 300 DPI 以上

2. **大文件处理**
   - OCR 是 CPU 密集型操作
   - 大文件会消耗较多时间和内存
   - 建议在服务器端处理，避免前端超时

3. **缓存策略**
   - 已处理的文档会被保存到 Milvus
   - 重复上传会自动跳过（文件名相同）

## 常见问题

### Q1: 提示 "OCR 环境未就绪"
**解决：**
- 确认 `tessdata` 文件夹存在
- 确认语言包文件已下载且完整
- 检查文件权限

### Q2: OCR 识别结果为空或乱码
**解决：**
- 检查 PDF 是否为真正的扫描件（全是图片）
- 尝试提高 DPI 值（如改为 400）
- 确认使用了正确的语言包
- 检查 PDF 图片质量

### Q3: OCR 处理速度太慢
**解决：**
- 降低 DPI 值（如改为 200）
- 减少同时处理的文件数量
- 升级硬件（CPU、内存）

### Q4: 内存溢出 (OutOfMemoryError)
**解决：**
- 增加 JVM 堆内存：`-Xmx4g`
- 降低 DPI 值
- 分批处理大文件

## 测试 OCR 功能

1. 准备一个扫描件 PDF 文件
2. 启动应用：`mvn spring-boot:run`
3. 访问 RAG 文档管理页面
4. 上传扫描件 PDF
5. 观察控制台日志：
   ```
   🔍 开始 OCR 处理流程: xxx.pdf
   开始 OCR 识别: xxx.pdf, DPI: 300, 语言: chi_sim+eng
   PDF 共 5 页，开始逐页 OCR 识别...
   第 1/5 页 OCR 完成，识别 1234 字符
   ...
   ✅ OCR 识别成功: xxx.pdf, 提取文本长度: 6789 字符
   ```

## 技术支持

如遇问题，请检查：
1. 应用日志中的详细错误信息
2. Tesseract 版本兼容性
3. 语言包完整性
4. 系统资源使用情况
