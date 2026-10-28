import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import { readFileSync } from 'node:fs'

const frontendVersion = JSON.parse(readFileSync(new URL('./package.json', import.meta.url), 'utf8')).version

// https://vite.dev/config/
export default defineConfig({
    define: { __RAG_FRONTEND_VERSION__: JSON.stringify(typeof frontendVersion === 'string' ? frontendVersion : '') },
    plugins: [vue()],
    resolve: {
        alias: {
            '@': resolve(__dirname, 'src'),
        },
    },
    server: {
        port: 5173,
        proxy: {
            '/mcp': {
                target: 'http://127.0.0.1:8080',
                changeOrigin: true,
            },
            '/auth': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
        },
    },
})
