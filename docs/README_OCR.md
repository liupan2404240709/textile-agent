# 扫描件PDF处理 - OCR功能

## 🎯 功能说明

系统现已支持**扫描件PDF**的自动识别和处理！

### 工作原理

1. **智能检测**: 上传PDF时，系统首先尝试直接提取文本
2. **自动回退**: 如果提取失败（空白文档），自动切换到OCR模式
3. **OCR识别**: 将PDF每页转为图片，使用Tesseract进行文字识别
4. **向量化存储**: 识别后的文本正常进行切分、向量化并存储到Milvus

### 支持的文档类型

✅ **文本型PDF** - 直接提取，速度快  
✅ **扫描件PDF** - 自动OCR识别  
✅ **图片型PDF** - 自动OCR识别  
✅ **Word文档** - 直接提取  
✅ **TXT/Markdown** - 直接提取  

---

## 🚀 快速开始

### Windows用户（一键设置）

```bash
# 1. 双击运行脚本下载语言包
download-ocr-langs.bat

# 2. 安装 Tesseract OCR
# 访问: https://github.com/UB-Mannheim/tesseract/wiki
# 下载并安装最新版本

# 3. 启动应用
mvn spring-boot:run

# 4. 上传扫描件PDF测试
```

### 手动设置

详见 [OCR_SETUP.md](OCR_SETUP.md)

---

## ⚙️ 配置选项

在 `application.yml` 中调整：

```yaml
rag:
  ocr:
    enabled: true              # 启用OCR
    auto-fallback: true        # 自动回退（推荐开启）
    language: chi_sim+eng      # 识别语言
    data-path: ./tessdata      # 语言包路径
    dpi: 300                   # PDF转图片DPI
```

### 性能调优

| 参数 | 低配置 | 推荐 | 高配置 |
|------|--------|------|--------|
| DPI | 200 | 300 | 400 |
| 适用场景 | 快速测试 | 日常使用 | 高精度需求 |
| 速度 | 快 | 中等 | 慢 |
| 识别率 | 一般 | 良好 | 优秀 |

---

## 📊 处理流程对比

### 文本型PDF
```
上传 → Tika解析 → 文本提取 → 切分 → 向量化 → 存储
耗时: ~1-5秒
```

### 扫描件PDF
```
上传 → Tika解析(失败) → OCR回退 → 逐页识别 → 文本合并 → 切分 → 向量化 → 存储
耗时: ~10-60秒 (取决于页数和质量)
```

---

## 💡 最佳实践

### 扫描件质量要求
- ✅ 清晰可读，无模糊
- ✅ 文字方向正确，无倾斜
- ✅ 光照均匀，无阴影
- ✅ 分辨率 ≥ 300 DPI

### 大文件处理建议
- 📌 单文件 < 50MB
- 📌 页数 < 100页（分批上传）
- 📌 服务器内存 ≥ 4GB
- 📌 避免并发上传多个大文件

### 提升识别准确率
1. 预处理PDF（去噪、纠偏、增强对比度）
2. 选择合适的语言包
3. 提高DPI值（300→400）
4. 确保扫描质量

---

## 🔍 日志示例

### 成功处理扫描件
```
🔍 开始 OCR 处理流程: medical_report.pdf
开始 OCR 识别: medical_report.pdf, DPI: 300, 语言: chi_sim+eng
PDF 共 5 页，开始逐页 OCR 识别...
第 1/5 页 OCR 完成，识别 1234 字符
第 2/5 页 OCR 完成，识别 987 字符
第 3/5 页 OCR 完成，识别 1456 字符
第 4/5 页 OCR 完成，识别 1123 字符
第 5/5 页 OCR 完成，识别 890 字符
OCR 识别完成: 总字符数=5690, 耗时=23456ms, 平均每页=4691ms
✅ OCR 识别成功: medical_report.pdf, 提取文本长度: 5690 字符
文档处理完成: medical_report.pdf, 生成 12 个文本片段, 耗时: 25678ms
```

### OCR环境未就绪
```
❌ OCR 环境未就绪，无法处理扫描件PDF: scan.pdf
请安装 Tesseract OCR 并下载语言包。
详情: OCR状态: 已启用 | 语言: chi_sim+eng | DPI: 300 | 数据路径: ./tessdata
```

---

## ❓ 常见问题

### Q: 如何判断我的PDF是扫描件？
**A:** 在PDF阅读器中尝试选中文字：
- 能选中复制 → 文本型PDF
- 无法选中 → 扫描件/图片型PDF

### Q: OCR识别率低怎么办？
**A:** 
1. 检查PDF扫描质量
2. 提高DPI值（改为400）
3. 确认语言包正确
4. 预处理PDF（去噪、纠偏）

### Q: 处理速度慢正常吗？
**A:** 正常。OCR是CPU密集型操作：
- 1页 ≈ 3-5秒
- 10页 ≈ 30-50秒
- 50页 ≈ 3-5分钟

### Q: 可以关闭OCR功能吗？
**A:** 可以，修改配置：
```yaml
rag:
  ocr:
    enabled: false
```

---

## 📚 相关文档

- [OCR环境安装指南](OCR_SETUP.md) - 详细安装步骤
- [RAG部署文档](RAG_DEPLOYMENT.md) - RAG系统整体说明
- [Tesseract官方文档](https://tesseract-ocr.github.io/) - OCR引擎文档

---

## 🆘 技术支持

遇到问题请提供：
1. 应用日志（包含错误信息）
2. PDF文件样本（如果可以分享）
3. 系统配置（OS、内存、Tesseract版本）
4. OCR环境检查结果
