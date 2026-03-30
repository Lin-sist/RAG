import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
    {
        path: '/login',
        component: () => import('@/layouts/AuthLayout.vue'),
        meta: { requiresAuth: false },
        children: [
            {
                path: '',
                name: 'Login',
                component: () => import('@/views/login/LoginView.vue'),
            },
        ],
    },
    {
        path: '/',
        component: () => import('@/layouts/AppShell.vue'),
        meta: { requiresAuth: true },
        redirect: '/chat',
        children: [
            {
                path: 'chat',
                name: 'Chat',
                component: () => import('@/components/chat/ChatPanel.vue'),
                meta: { title: '新会话' },
            },
            {
                path: 'chat/:id',
                name: 'ChatSession',
                component: () => import('@/components/chat/ChatPanel.vue'),
                meta: { title: '历史会话' },
            },
            {
                path: 'chat-v2',
                name: 'ChatV2',
                component: () => import('@/components/chat/ChatPanel.vue'),
                meta: { title: '新会话' },
            },
            {
                path: 'kb',
                name: 'KnowledgeBaseList',
                component: () => import('@/views/knowledge-base/KnowledgeBaseList.vue'),
                meta: { title: '知识库列表' },
            },
            {
                path: 'kb/:id',
                name: 'KnowledgeBaseDetail',
                component: () => import('@/views/knowledge-base/KnowledgeBaseDetail.vue'),
                meta: { title: '知识库详情' },
            },
            {
                path: 'history',
                name: 'History',
                component: () => import('@/views/history/ChatHistory.vue'),
                meta: { title: '历史记录' },
            },
        ],
    },

    //临时测试v0
    // 历史记录页面兼容入口
    {
        path: '/history-v2',
        name: 'ChatHistory',
        redirect: '/history',
    },

    // 兼容旧知识库路径
    {
        path: '/knowledge-base',
        redirect: '/kb',
    },
    {
        path: '/knowledge-base/:id',
        redirect: (to) => `/kb/${to.params.id}`,
    },

    // 个人资料页面兼容入口
    {
        path: '/profile',
        name: 'UserProfile',
        redirect: '/chat',
    },

    {
        path: '/:pathMatch(.*)*',
        redirect: '/login',
    },
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

// 导航守卫：未登录跳转到登录页
router.beforeEach((to, _from, next) => {
    const authStore = useAuthStore()

    if (to.meta.requiresAuth !== false && !authStore.isLoggedIn) {
        next({ name: 'Login', query: { redirect: to.fullPath } })
    } else if (to.name === 'Login' && authStore.isLoggedIn) {
        next({ path: '/' })
    } else {
        next()
    }
})

export default router
