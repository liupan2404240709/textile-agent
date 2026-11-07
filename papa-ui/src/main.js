import { createApp } from 'vue';
import App from './App.vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import './style.css';
import router from './router';
import axios from 'axios';
import { getToken } from '@/utils';

// 配置全局 axios 拦截器
axios.interceptors.request.use(
    config => {
        const token = getToken()
        if (token) {
            // 同时设置 Authorization 和 access-token header
            config.headers['Authorization'] = `Bearer ${token}`
            config.headers['access-token'] = token
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

const app = createApp(App);
app.use(ElementPlus);
app.use(router);
app.mount('#app');



