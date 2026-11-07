import axios from 'axios'
import { getToken } from '@/utils'

const request = axios.create({
    baseURL: '/api',
    timeout: 300000  // 5分钟超时，大文件上传和向量化需要更长时间
})

request.interceptors.request.use(
    config => {
        const token = getToken()
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
            // 添加 access-token header 用于后端拦截器认证
            config.headers['access-token'] = token
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

request.interceptors.response.use(
    response => {
        return response.data
    },
    error => {
        console.error('请求错误:', error)
        return Promise.reject(error)
    }
)

// 上传单个文档
export function uploadDocument(file) {
    const formData = new FormData()
    formData.append('file', file)
    return request({
        url: '/rag/upload',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}

// 批量上传文档
export function uploadBatchDocuments(files) {
    const formData = new FormData()
    files.forEach(file => {
        formData.append('files', file)
    })
    return request({
        url: '/rag/upload-batch',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}

// 获取已上传文档列表
export function getDocuments() {
    return request({
        url: '/rag/documents',
        method: 'get'
    })
}

// 删除文档
export function deleteDocument(fileName) {
    return request({
        url: `/rag/document/${fileName}`,
        method: 'delete'
    })
}

// 获取处理日志
export function getLogs(count = 50) {
    return request({
        url: '/rag/logs',
        method: 'get',
        params: { count }
    })
}

// 清空日志
export function clearLogs() {
    return request({
        url: '/rag/logs',
        method: 'delete'
    })
}
