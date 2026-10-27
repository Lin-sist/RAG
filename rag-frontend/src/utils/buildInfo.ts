declare const __RAG_FRONTEND_VERSION__: string
const version = typeof __RAG_FRONTEND_VERSION__ === 'string' ? __RAG_FRONTEND_VERSION__ : ''
export const FRONTEND_VERSION = !version ? '版本信息未提供' : /^0\.0\.0(?:-|$)/.test(version) ? '开发版本' : version
