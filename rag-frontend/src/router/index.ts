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
        component: () => import('@/layouts/DefaultLayout.vue'),
        meta: { requiresAuth: true },
        children: [
            {
                path: '',
                redirect: '/knowledge-base',
            },
            {
                path: 'knowledge-base',
                name: 'KBList',
                component: () => import('@/views/knowledge-base/KBListView.vue'),
                meta: { title: '知识库管理' },
            },
            {
                path: 'knowledge-base/:id',
                name: 'KBDetail',
                component: () => import('@/views/knowledge-base/KBDetailView.vue'),
                meta: { title: '知识库详情' },
            },
            {
                path: 'chat',
                name: 'Chat',
                component: () => import('@/views/chat/ChatView.vue'),
                meta: { title: '智能问答' },
            },
            {
                path: 'history',
                name: 'History',
                component: () => import('@/views/history/HistoryView.vue'),
                meta: { title: '问答历史' },
            },
        ],
    },

    //临时测试v0
    {
        path: '/chat-v2',
        name: 'ChatV2',
        component: () => import('@/components/chat/RagChatInterface.vue')
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
