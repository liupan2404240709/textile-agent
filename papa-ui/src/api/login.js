import axios from 'axios'
import { getToken } from '@/utils'

const request = axios.create({
    baseURL: '/api/admin',
    timeout: 10000
})

request.interceptors.request.use(
    config => {
        const token = getToken()
        if (token) {
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
        const res = response.data
        if (res.code !== 200) {
            return Promise.reject(new Error(res.message || 'Error'))
        }
        return res
    },
    error => {
        console.error('请求错误:', error)
        return Promise.reject(error)
    }
)

export function getCode() {
    return request({
        url: '/login/captcha',
        method: 'get'
    })
}

export function login(data) {
    return request({
        url: '/login',
        method: 'post',
        data
    })
}

export function getUserInfo() {
    return request({
        url: '/info',
        method: 'get'
    })
}
