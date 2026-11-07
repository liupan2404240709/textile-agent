import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/components/login/index.vue')
    },
    {
        path: '/',
        component: () => import('@/components/layout/Layout.vue'),
        redirect: '/chat',
        meta: { requiresAuth: true },
        children: [
            {
                path: 'chat',
                name: 'Home',
                component: () => import('@/components/chatwindow/ChatWindow.vue')
            },
            {
                path: 'rag',
                name: 'RagDocument',
                component: () => import('@/components/rag/RagDocument.vue')
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    const token = getToken()
    
    // 如果访问登录页且已有token，直接跳转到首页
    if (to.path === '/login' && token) {
        next('/chat')
        return
    }
    
    // 如果需要认证且没有token，跳转到登录页
    if (to.meta.requiresAuth && !token) {
        next('/login')
        return
    }
    
    // 其他情况正常放行
    next()
})

export default router
