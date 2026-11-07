<template>
    <div class="rag-container">
        <!-- 头部卡片 -->
        <el-card class="header-card" shadow="hover">
            <div class="header-content">
                <div class="header-left">
                    <div class="title-wrapper">
                        <div class="icon-wrapper">
                            <el-icon :size="32"><Document /></el-icon>
                        </div>
                        <div>
                            <h2>RAG 文档管理</h2>
                            <p class="description">智能知识库管理系统 · 增强检索生成</p>
                        </div>
                    </div>
                </div>
                <el-button type="primary" class="back-btn" @click="$router.push('/chat')">
                    <el-icon><ChatDotRound /></el-icon>
                    <span>返回对话</span>
                </el-button>
            </div>
        </el-card>

        <!-- 主内容区：左侧上传，右侧文档与日志 -->
        <div class="dashboard-grid">
            <!-- 上传区域 -->
            <el-card class="upload-card" shadow="hover">
                <div class="card-header">
                    <div class="header-title">
                        <el-icon class="title-icon"><UploadFilled /></el-icon>
                        <h3>上传文档</h3>
                    </div>
                    <el-tag type="info" size="small">支持批量上传</el-tag>
                </div>
                
                <el-upload
                    ref="uploadRef"
                    class="upload-demo"
                    drag
                    :auto-upload="false"
                    :on-change="handleFileChange"
                    :on-remove="handleFileRemove"
                    :before-upload="beforeUpload"
                    multiple
                    accept=".pdf,.txt,.md,.doc,.docx"
                >
                    <el-icon class="el-icon--upload" :size="48"><upload-filled /></el-icon>
                    <div class="el-upload__text">
                        <p class="upload-main-text">拖拽文件到此处，或 <em>点击上传</em></p>
                        <p class="upload-sub-text">支持 PDF、TXT、Markdown、Word 格式</p>
                    </div>
                    <template #tip>
                        <div class="el-upload__tip">
                            <el-icon><InfoFilled /></el-icon>
                            <span>单个文件不超过 50MB。✅ 支持扫描件PDF（自动OCR识别）</span>
                        </div>
                    </template>
                </el-upload>

                <!-- 上传进度条 -->
                <div v-if="uploading" class="upload-progress">
                    <div class="progress-header">
                        <el-icon class="is-loading"><Loading /></el-icon>
                        <span class="progress-label">正在处理文档...</span>
                    </div>
                    <el-progress 
                        :percentage="uploadProgress" 
                        :status="uploadProgress === 100 ? 'success' : null"
                        :stroke-width="8"
                    />
                    <p class="progress-text">
                        已上传: <strong>{{ uploadedCount }}</strong> / {{ totalFiles }} 个文件
                    </p>
                </div>

                <div class="upload-actions">
                    <el-button 
                        type="primary" 
                        size="large"
                        :disabled="uploadFileList.length === 0 || uploading"
                        :loading="uploading"
                        @click="submitUpload"
                        class="upload-btn"
                    >
                        <el-icon v-if="!uploading"><Upload /></el-icon>
                        <span>{{ uploading ? '上传中...' : `开始上传 (${uploadFileList.length})` }}</span>
                    </el-button>
                    <el-button 
                        size="large"
                        @click="clearFiles" 
                        :disabled="uploading || uploadFileList.length === 0"
                        class="clear-btn"
                    >
                        <el-icon><Delete /></el-icon>
                        <span>清空列表</span>
                    </el-button>
                </div>
            </el-card>

            <div class="side-panel">
                <!-- 文档列表 -->
                <el-card class="documents-card" shadow="hover">
                    <div class="documents-header">
                        <div class="header-title">
                            <el-icon class="title-icon"><FolderOpened /></el-icon>
                            <h3>已上传文档</h3>
                            <el-badge :value="documentList.length" :max="999" class="doc-count-badge" />
                        </div>
                        <el-tooltip content="刷新文档列表" placement="top">
                            <el-button 
                                :icon="Refresh" 
                                circle 
                                size="default" 
                                @click="loadDocuments"
                                :loading="loading"
                                class="refresh-btn"
                            />
                        </el-tooltip>
                    </div>

                    <div class="table-scroll-area">
                        <el-table
                            v-loading="loading"
                            :data="documentList"
                            style="width: 100%"
                            empty-text="暂无上传的文档"
                            :row-class-name="'document-row'"
                            highlight-current-row
                        >
                            <el-table-column prop="name" label="文件名" min-width="250">
                                <template #default="{ row }">
                                    <div class="file-name">
                                        <div class="file-icon">
                                            <el-icon :size="20"><Document /></el-icon>
                                        </div>
                                        <div class="file-info">
                                            <span class="file-name-text">{{ row }}</span>
                                            <span class="file-type">{{ getFileType(row) }}</span>
                                        </div>
                                    </div>
                                </template>
                            </el-table-column>
                            
                            <el-table-column label="操作" width="140" align="center">
                                <template #default="{ row }">
                                    <el-popconfirm
                                        title="确定要删除此文档吗？"
                                        confirm-button-text="确定"
                                        cancel-button-text="取消"
                                        @confirm="handleDelete(row)"
                                    >
                                        <template #reference>
                                            <el-button
                                                type="danger"
                                                size="small"
                                                :icon="Delete"
                                                class="delete-btn"
                                            >
                                                删除
                                            </el-button>
                                        </template>
                                    </el-popconfirm>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                </el-card>

                <!-- 处理日志 -->
                <el-card class="logs-card" shadow="hover">
                    <div class="logs-header">
                        <div class="header-title">
                            <el-icon class="title-icon"><List /></el-icon>
                            <h3>处理日志</h3>
                            <el-tag type="success" size="small" effect="plain">实时记录</el-tag>
                        </div>
                        <div class="logs-actions">
                            <el-tooltip content="刷新日志" placement="top">
                                <el-button 
                                    :icon="Refresh" 
                                    size="default" 
                                    @click="loadLogs"
                                    :loading="logsLoading"
                                >
                                    刷新
                                </el-button>
                            </el-tooltip>
                            <el-popconfirm
                                title="确定要清空所有日志吗？"
                                confirm-button-text="确定"
                                cancel-button-text="取消"
                                @confirm="handleClearLogs"
                            >
                                <template #reference>
                                    <el-button 
                                        :icon="Delete" 
                                        size="default" 
                                        type="danger"
                                        plain
                                    >
                                        清空
                                    </el-button>
                                </template>
                            </el-popconfirm>
                        </div>
                    </div>

                    <div class="table-scroll-area">
                        <el-table
                            v-loading="logsLoading"
                            :data="logList"
                            style="width: 100%"
                            empty-text="暂无处理日志"
                            :row-class-name="getRowClassName"
                        >
                            <el-table-column prop="operation" label="操作类型" width="120" align="center">
                                <template #default="{ row }">
                                    <el-tag :type="getOperationTagType(row.operation)" size="small">
                                        {{ getOperationText(row.operation) }}
                                    </el-tag>
                                </template>
                            </el-table-column>
                            
                            <el-table-column prop="fileName" label="文件名" min-width="150" show-overflow-tooltip />
                            
                            <el-table-column prop="status" label="状态" width="100" align="center">
                                <template #default="{ row }">
                                    <el-tag :type="getStatusTagType(row.status)" size="small">
                                        {{ getStatusText(row.status) }}
                                    </el-tag>
                                </template>
                            </el-table-column>
                            
                            <el-table-column prop="message" label="消息" min-width="200" show-overflow-tooltip />
                            
                            <el-table-column prop="segmentCount" label="片段数" width="80" align="center">
                                <template #default="{ row }">
                                    <span v-if="row.segmentCount">{{ row.segmentCount }}</span>
                                    <span v-else>-</span>
                                </template>
                            </el-table-column>
                            
                            <el-table-column prop="duration" label="耗时" width="100" align="center">
                                <template #default="{ row }">
                                    <span v-if="row.duration">{{ formatDuration(row.duration) }}</span>
                                    <span v-else>-</span>
                                </template>
                            </el-table-column>
                            
                            <el-table-column prop="createTime" label="时间" width="160" align="center">
                                <template #default="{ row }">
                                    {{ formatTime(row.createTime) }}
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                </el-card>
            </div>
        </div>

        <!-- 使用说明 -->
        <el-card class="info-card">
            <h3>使用说明</h3>
            <el-alert
                title="RAG 工作流程"
                type="info"
                :closable="false"
                show-icon
            >
                <ol style="margin: 10px 0; padding-left: 20px; line-height: 1.8;">
                    <li><strong>上传文档</strong>：将 PDF、TXT、Markdown、Word 等格式的文档上传到知识库</li>
                    <li><strong>智能解析</strong>：系统会自动检测文档类型，文本型PDF直接提取，扫描件自动进行OCR识别</li>
                    <li><strong>文本切分</strong>：智能切分文本片段（每段 500 字符，重叠 50 字符）</li>
                    <li><strong>向量化</strong>：使用通义千问 Embedding 模型（text-embedding-v3）将文本转换为向量</li>
                    <li><strong>持久化存储</strong>：向量数据实时存储到 Milvus 向量数据库，支持数据持久化</li>
                    <li><strong>智能检索</strong>：在对话时自动从知识库中检索相关内容，增强回答准确性和专业性</li>
                </ol>
            </el-alert>

            <el-alert
                title="注意事项"
                type="success"
                :closable="false"
                show-icon
                style="margin-top: 15px;"
            >
                <ul style="margin: 10px 0; padding-left: 20px; line-height: 1.8;">
                    <li><strong>✅ OCR 功能已启用</strong>，支持扫描件PDF自动识别（中文+英文）</li>
                    <li><strong>✅ Milvus 向量数据库已启用</strong>，数据将持久化保存，重启后不会丢失</li>
                    <li>请确保 Milvus 服务正常运行（默认端口 19530）</li>
                    <li><strong>📌 OCR 说明：</strong>首次使用扫描件需要安装 Tesseract OCR，详见项目根目录 <code>OCR_SETUP.md</code></li>
                    <li>大文件上传和向量化可能需要较长时间，请耐心等待</li>
                    <li>上传的文档会同时保存到本地文件夹 <code>./documents/rag_uploads</code></li>
                    <li>建议在对话页面顶部点击 "RAG 文档管理" 按钮快速访问此页面</li>
                    <li>处理日志会记录所有操作的详细信息，包括成功/失败状态、耗时、片段数等</li>
                </ul>
            </el-alert>
        </el-card>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElProgress } from 'element-plus'
import { Upload, UploadFilled, Document, Delete, Refresh, ChatDotRound, InfoFilled, Loading, FolderOpened, List } from '@element-plus/icons-vue'
import { uploadDocument, getDocuments, deleteDocument, getLogs, clearLogs } from '@/api/rag'
import { getToken } from '@/utils'

const router = useRouter()
const uploadRef = ref(null)
const uploading = ref(false)
const loading = ref(false)
const logsLoading = ref(false)
const documentList = ref([])
const logList = ref([])
const uploadFileList = ref([]) // 上传文件列表
const uploadProgress = ref(0)
const totalFiles = ref(0)
const uploadedCount = ref(0)
const milvusEnabled = ref(true) // 已启用 Milvus 向量数据库

// 上传配置
// 上传前验证
const beforeUpload = (file) => {
    const isValidSize = file.size / 1024 / 1024 < 50
    if (!isValidSize) {
        ElMessage.error('文件大小不能超过 50MB!')
        return false
    }
    
    // 检查文件扩展名
    const fileName = file.name.toLowerCase()
    const validExtension = fileName.endsWith('.pdf') || 
                          fileName.endsWith('.txt') || 
                          fileName.endsWith('.md') ||
                          fileName.endsWith('.doc') ||
                          fileName.endsWith('.docx')
    
    if (!validExtension) {
        ElMessage.error('只支持 PDF、TXT、Markdown、Word 格式的文档!')
        return false
    }
    
    return true
}

// 提交上传
const submitUpload = async () => {
    if (uploadFileList.value.length === 0) {
        ElMessage.warning('请先选择要上传的文件')
        return
    }

    uploading.value = true
    uploadProgress.value = 0
    totalFiles.value = uploadFileList.value.length
    uploadedCount.value = 0
    
    let successCount = 0
    let failCount = 0
    const failedFiles = []
    
    try {
        // 逐个上传文件并更新进度
        for (let i = 0; i < uploadFileList.value.length; i++) {
            const file = uploadFileList.value[i]
            try {
                await uploadDocument(file.raw)
                successCount++
                uploadedCount.value = i + 1
                uploadProgress.value = Math.round(((i + 1) / uploadFileList.value.length) * 100)
            } catch (error) {
                failCount++
                failedFiles.push(file.name)
                console.error(`上传失败: ${file.name}`, error)
            }
        }
        
        // 显示上传结果
        if (failCount === 0) {
            ElMessage.success({
                message: `✅ 成功上传 ${successCount} 个文档`,
                duration: 3000
            })
        } else if (successCount > 0) {
            ElMessage.warning({
                message: `⚠️ 部分上传成功：成功 ${successCount} 个，失败 ${failCount} 个。请检查失败文件是否为空白或损坏的PDF`,
                duration: 5000
            })
            if (failedFiles.length > 0) {
                console.warn('失败的文件:', failedFiles)
            }
        } else {
            ElMessage.error({
                message: `❌ 所有文件上传失败。可能原因：1)文件为空白/扫描件PDF；2)文件损坏；3)文件被加密。建议使用可复制文本的PDF文件`,
                duration: 6000
            })
        }
        
        uploadFileList.value = []
        uploadRef.value?.clearFiles()
        uploadProgress.value = 0
        
        // 刷新文档列表和日志
        await loadDocuments()
        await loadLogs()
    } catch (error) {
        console.error('上传过程出错:', error)
        ElMessage.error('上传过程出错，请重试')
    } finally {
        uploading.value = false
        totalFiles.value = 0
        uploadedCount.value = 0
    }
}

// 文件列表变化回调
const handleFileChange = (file, fileList) => {
    uploadFileList.value = fileList
}

// 文件移除回调
const handleFileRemove = (file, fileList) => {
    uploadFileList.value = fileList
}

// 清空文件列表
const clearFiles = () => {
    uploadRef.value?.clearFiles()
    uploadFileList.value = []
    uploadProgress.value = 0
}

// 加载文档列表
const loadDocuments = async () => {
    loading.value = true
    try {
        const response = await getDocuments()
        if (response.code === 200) {
            documentList.value = response.data || []
        } else {
            ElMessage.error(response.message || '加载文档列表失败')
        }
    } catch (error) {
        console.error('加载文档列表失败:', error)
        ElMessage.error('加载文档列表失败，请检查网络连接')
    } finally {
        loading.value = false
    }
}

// 删除文档
const handleDelete = (fileName) => {
    ElMessageBox.confirm(
        `确定要删除文档 "${fileName}" 吗？`,
        '警告',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        }
    ).then(async () => {
        try {
            const response = await deleteDocument(fileName)
            if (response.code === 200) {
                ElMessage.success('删除成功')
                await loadDocuments()
            } else {
                ElMessage.error(response.message || '删除失败')
            }
        } catch (error) {
            console.error('删除文档失败:', error)
            ElMessage.error('删除文档失败，请重试')
        }
    }).catch(() => {
        // 用户取消删除
    })
}

// 获取文件类型
const getFileType = (fileName) => {
    const ext = fileName.split('.').pop()?.toLowerCase()
    const types = {
        'pdf': 'PDF文档',
        'txt': '文本文件',
        'md': 'Markdown',
        'doc': 'Word文档',
        'docx': 'Word文档'
    }
    return types[ext] || '未知类型'
}

// 格式化文件大小
const formatFileSize = (bytes) => {
    if (!bytes || bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 加载日志列表
const loadLogs = async () => {
    logsLoading.value = true
    try {
        const response = await getLogs(50)
        if (response.code === 200) {
            logList.value = response.data || []
        } else {
            ElMessage.error(response.message || '加载日志失败')
        }
    } catch (error) {
        console.error('加载日志失败:', error)
        ElMessage.error('加载日志失败，请检查网络连接')
    } finally {
        logsLoading.value = false
    }
}

// 清空日志
const handleClearLogs = () => {
    ElMessageBox.confirm(
        '确定要清空所有处理日志吗？',
        '警告',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        }
    ).then(async () => {
        try {
            const response = await clearLogs()
            if (response.code === 200) {
                ElMessage.success('日志已清空')
                await loadLogs()
            } else {
                ElMessage.error(response.message || '清空日志失败')
            }
        } catch (error) {
            console.error('清空日志失败:', error)
            ElMessage.error('清空日志失败，请重试')
        }
    }).catch(() => {
        // 用户取消
    })
}

// 获取操作类型标签样式
const getOperationTagType = (operation) => {
    const types = {
        'UPLOAD': 'primary',
        'DELETE': 'danger',
        'BATCH_UPLOAD': 'success'
    }
    return types[operation] || 'info'
}

// 获取操作类型文本
const getOperationText = (operation) => {
    const texts = {
        'UPLOAD': '上传',
        'DELETE': '删除',
        'BATCH_UPLOAD': '批量上传'
    }
    return texts[operation] || operation
}

// 获取状态标签样式
const getStatusTagType = (status) => {
    const types = {
        'SUCCESS': 'success',
        'ERROR': 'danger',
        'PROCESSING': 'warning'
    }
    return types[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
    const texts = {
        'SUCCESS': '成功',
        'ERROR': '失败',
        'PROCESSING': '处理中'
    }
    return texts[status] || status
}

// 获取行类名
const getRowClassName = ({ row }) => {
    if (row.status === 'ERROR') {
        return 'error-row'
    } else if (row.status === 'SUCCESS') {
        return 'success-row'
    } else if (row.status === 'PROCESSING') {
        return 'processing-row'
    }
    return ''
}

// 格式化耗时
const formatDuration = (duration) => {
    if (!duration) return '-'
    if (duration < 1000) {
        return `${duration}ms`
    }
    return `${(duration / 1000).toFixed(2)}s`
}

// 格式化时间
const formatTime = (timeStr) => {
    if (!timeStr) return '-'
    const date = new Date(timeStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 组件挂载时加载文档列表和日志
onMounted(() => {
    loadDocuments()
    loadLogs()
})
</script>

<style scoped lang="scss">
.rag-container {
    padding: 20px;
    max-width: 1800px;
    margin: 0 auto;
    background: #ffffff;
    min-height: 100vh;

    // 头部卡片 - 黑白简约
    .header-card {
        margin-bottom: 20px;
        border-radius: 8px;
        border: 1px solid #e0e0e0;
        background: #000000;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

        :deep(.el-card__body) {
            padding: 20px 24px;
        }

        .header-content {
            display: flex;
            justify-content: space-between;
            align-items: center;

            .header-left {
                flex: 1;
            }

            .title-wrapper {
                display: flex;
                align-items: center;
                gap: 16px;

                .icon-wrapper {
                    width: 48px;
                    height: 48px;
                    border-radius: 8px;
                    background: rgba(255, 255, 255, 0.1);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: white;
                }
            }
        }

        h2 {
            margin: 0 0 4px 0;
            color: white;
            font-size: 22px;
            font-weight: 600;
        }

        .description {
            margin: 0;
            color: rgba(255, 255, 255, 0.7);
            font-size: 13px;
        }

        .back-btn {
            background: white;
            border: none;
            color: #000000;
            font-weight: 500;
            padding: 10px 20px;
            border-radius: 6px;
            transition: all 0.2s ease;

            &:hover {
                background: #f5f5f5;
            }
        }
    }

    .dashboard-grid {
        display: grid;
        grid-template-columns: minmax(420px, 0.95fr) minmax(560px, 1.25fr);
        gap: 20px;
        height: calc(100vh - 140px);
        min-height: 620px;
        margin-bottom: 20px;
        align-items: stretch;
    }

    .side-panel {
        min-height: 0;
        display: grid;
        grid-template-rows: minmax(240px, 0.9fr) minmax(300px, 1.1fr);
        gap: 20px;
        overflow: hidden;
    }

    .table-scroll-area {
        flex: 1;
        min-height: 0;
        overflow: auto;
        border: 1px solid #f0f0f0;
        border-radius: 8px;
        background: #ffffff;
        scrollbar-width: thin;
        scrollbar-color: #bdbdbd #f5f5f5;

        &::-webkit-scrollbar {
            width: 8px;
            height: 8px;
        }

        &::-webkit-scrollbar-track {
            background: #f5f5f5;
            border-radius: 8px;
        }

        &::-webkit-scrollbar-thumb {
            background: #bdbdbd;
            border-radius: 8px;

            &:hover {
                background: #8c8c8c;
            }
        }
    }

    // 上传卡片
    .upload-card {
        margin-bottom: 0;
        border-radius: 12px;
        border: 1px solid #e8e8e8;
        box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
        overflow: hidden;
        display: flex;
        flex-direction: column;
        min-height: 0;
        background: linear-gradient(180deg, #ffffff 0%, #fcfcfc 100%);

        :deep(.el-card__body) {
            display: flex;
            flex-direction: column;
            flex: 1;
            min-height: 0;
            padding: 22px 24px;
        }

        .card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 16px;
            padding-bottom: 12px;
            border-bottom: 1px solid #f0f0f0;

            .header-title {
                display: flex;
                align-items: center;
                gap: 8px;

                .title-icon {
                    font-size: 18px;
                    color: #000000;
                }

                h3 {
                    margin: 0;
                    color: #000000;
                    font-size: 16px;
                    font-weight: 600;
                }
            }
        }

        .upload-demo {
            margin-bottom: 16px;

            :deep(.el-upload-dragger) {
                padding: 32px 24px;
                border: 2px dashed #d9d9d9;
                border-radius: 6px;
                background: #fafafa;
                transition: all 0.2s ease;

                &:hover {
                    border-color: #000000;
                    background: #ffffff;
                }

                .el-icon--upload {
                    color: #000000;
                    margin-bottom: 12px;
                }

                .el-upload__text {
                    .upload-main-text {
                        margin: 0 0 6px 0;
                        color: #333333;
                        font-size: 14px;

                        em {
                            color: #000000;
                            font-style: normal;
                            font-weight: 600;
                        }
                    }

                    .upload-sub-text {
                        margin: 0;
                        color: #999999;
                        font-size: 12px;
                    }
                }
            }

            :deep(.el-upload__tip) {
                display: flex;
                align-items: center;
                gap: 6px;
                margin-top: 10px;
                color: #666666;
                font-size: 12px;
                justify-content: center;

                .el-icon {
                    color: #000000;
                }
            }
        }

        .upload-progress {
            margin: 16px 0;
            padding: 16px;
            background: #f5f5f5;
            border-radius: 6px;
            border: 1px solid #e0e0e0;

            .progress-header {
                display: flex;
                align-items: center;
                gap: 8px;
                margin-bottom: 10px;

                .el-icon {
                    color: #000000;
                    font-size: 16px;
                }

                .progress-label {
                    color: #333333;
                    font-weight: 500;
                    font-size: 13px;
                }
            }

            .progress-text {
                margin: 10px 0 0 0;
                text-align: center;
                color: #333333;
                font-size: 13px;

                strong {
                    color: #000000;
                    font-weight: 600;
                }
            }
        }

        .upload-actions {
            display: flex;
            gap: 10px;
            justify-content: flex-end;
            margin-top: auto;
            padding-top: 18px;
            border-top: 1px solid #f2f2f2;

            .upload-btn,
            .clear-btn {
                min-width: 120px;
                border-radius: 6px;
                font-weight: 500;
                transition: all 0.2s ease;

                &:hover {
                    transform: translateY(-1px);
                }
            }

            .upload-btn {
                background: #000000;
                border-color: #000000;
                box-shadow: none;

                &:hover {
                    background: #333333;
                    border-color: #333333;
                }
            }
        }
    }

    // 文档列表卡片
    .documents-card {
        margin-bottom: 0;
        border-radius: 12px;
        border: 1px solid #e8e8e8;
        box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
        overflow: hidden;
        display: flex;
        flex-direction: column;
        min-height: 0;
        background: linear-gradient(180deg, #ffffff 0%, #fcfcfc 100%);

        :deep(.el-card__body) {
            display: flex;
            flex-direction: column;
            flex: 1;
            min-height: 0;
            padding: 20px 22px 22px;
        }

        .documents-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 14px;
            padding-bottom: 12px;
            border-bottom: 1px solid #f0f0f0;
            flex-shrink: 0;

            .header-title {
                display: flex;
                align-items: center;
                gap: 8px;

                .title-icon {
                    font-size: 18px;
                    color: #000000;
                }

                h3 {
                    margin: 0;
                    color: #000000;
                    font-size: 16px;
                    font-weight: 600;
                }

                .doc-count-badge {
                    :deep(.el-badge__content) {
                        background: #000000;
                        border: 2px solid white;
                    }
                }
            }

            .refresh-btn {
                transition: all 0.3s ease;

                &:hover {
                    transform: rotate(180deg);
                }
            }
        }

        :deep(.el-table) {
            height: 100%;

            .el-table__inner-wrapper::before {
                display: none;
            }
        }

        :deep(.el-table__body-wrapper) {
            scrollbar-width: thin;
            scrollbar-color: #bdbdbd #f5f5f5;
        }

        :deep(.el-scrollbar__bar.is-vertical) {
            right: 4px;
        }

        .file-name {
            display: flex;
            align-items: center;
            gap: 10px;
            
            .file-icon {
                width: 36px;
                height: 36px;
                border-radius: 6px;
                background: #000000;
                display: flex;
                align-items: center;
                justify-content: center;
                color: white;
            }

            .file-info {
                display: flex;
                flex-direction: column;
                gap: 2px;

                .file-name-text {
                    color: #000000;
                    font-weight: 500;
                    font-size: 13px;
                }

                .file-type {
                    color: #999999;
                    font-size: 11px;
                }
            }
        }

        .delete-btn {
            border-radius: 6px;
            transition: all 0.2s ease;

            &:hover {
                transform: scale(1.05);
            }
        }

        :deep(.document-row) {
            transition: all 0.2s ease;

            &:hover {
                background-color: #f5f5f5 !important;
            }
        }
    }

    // 日志卡片
    .logs-card {
        margin-bottom: 0;
        border-radius: 12px;
        border: 1px solid #e8e8e8;
        box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
        overflow: hidden;
        display: flex;
        flex-direction: column;
        min-height: 0;
        background: linear-gradient(180deg, #ffffff 0%, #fcfcfc 100%);

        :deep(.el-card__body) {
            display: flex;
            flex-direction: column;
            flex: 1;
            min-height: 0;
            padding: 20px 22px 22px;
        }

        .logs-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 14px;
            padding-bottom: 12px;
            border-bottom: 1px solid #f0f0f0;
            flex-shrink: 0;

            .header-title {
                display: flex;
                align-items: center;
                gap: 8px;

                .title-icon {
                    font-size: 18px;
                    color: #000000;
                }

                h3 {
                    margin: 0;
                    color: #000000;
                    font-size: 16px;
                    font-weight: 600;
                }
            }

            .logs-actions {
                display: flex;
                gap: 8px;
            }
        }

        :deep(.el-table) {
            height: 100%;

            .el-table__inner-wrapper::before {
                display: none;
            }

            .el-table__header th {
                background: #fafafa;
                color: #000000;
                font-weight: 600;
                border-bottom: 1px solid #e0e0e0;
            }

            .el-table__row {
                &:hover {
                    background-color: #f5f5f5 !important;
                }
            }
        }

        .error-row {
            background-color: #fff2f0 !important;
        }

        .success-row {
            background-color: #f6ffed !important;
        }
    }

    // 信息卡片
    .info-card {
        border-radius: 8px;
        border: 1px solid #e0e0e0;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
        background: #fafafa;

        h3 {
            margin: 0 0 16px 0;
            color: #000000;
            font-size: 16px;
            font-weight: 600;
        }

        :deep(.el-alert) {
            background: white;
            border: 1px solid #e0e0e0;
            border-radius: 6px;
            padding: 12px 16px;

            .el-alert__title {
                color: #000000;
                font-weight: 600;
                font-size: 14px;
            }

            .el-alert__content {
                color: #666666;
                font-size: 13px;
            }
        }

        ol, ul {
            margin: 8px 0;
            padding-left: 20px;
            line-height: 1.8;
            color: #666666;
            font-size: 13px;

            li {
                margin: 4px 0;
            }

            strong {
                color: #000000;
                font-weight: 600;
            }

            code {
                background: #f0f0f0;
                padding: 2px 6px;
                border-radius: 3px;
                color: #000000;
                font-size: 12px;
            }
        }
    }
}
</style>

// 响应式设计
@media (max-width: 1400px) {
    .rag-container {
        .dashboard-grid {
            grid-template-columns: 1fr;
            height: auto;
        }

        .side-panel {
            grid-template-rows: repeat(2, minmax(320px, auto));
        }
    }
}

@media (max-width: 992px) {
    .rag-container {
        padding: 16px;

        .dashboard-grid {
            grid-template-columns: 1fr;
            min-height: auto;
        }

        .upload-card,
        .documents-card,
        .logs-card {
            min-height: auto;
        }

        .documents-header,
        .logs-header,
        .header-content {
            flex-direction: column;
            align-items: flex-start;
            gap: 12px;
        }

        .logs-actions,
        .upload-actions {
            width: 100%;
            flex-wrap: wrap;
        }
    }
}