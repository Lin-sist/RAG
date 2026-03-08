import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { KnowledgeBaseDTO, CreateKBRequest, UpdateKBRequest, KnowledgeBaseStatistics } from '@/types/knowledgeBase'
import * as kbApi from '@/api/knowledgeBase'

export const useKnowledgeBaseStore = defineStore('knowledgeBase', () => {
    // ---------- 状态 ----------
    const list = ref<KnowledgeBaseDTO[]>([])
    const current = ref<KnowledgeBaseDTO | null>(null)
    const statistics = ref<KnowledgeBaseStatistics | null>(null)
    const loading = ref(false)

    // ---------- 加载知识库列表 ----------
    async function fetchList() {
        loading.value = true
        try {
            const res = await kbApi.listKB()
            list.value = res.data.data
        } catch (e: any) {
            ElMessage.error('获取知识库列表失败')
            throw e
        } finally {
            loading.value = false
        }
    }

    // ---------- 加载单个知识库详情 ----------
    async function fetchById(id: number) {
        loading.value = true
        try {
            const res = await kbApi.getKBById(id)
            current.value = res.data.data
            return res.data.data
        } catch (e: any) {
            ElMessage.error('获取知识库详情失败')
            throw e
        } finally {
            loading.value = false
        }
    }

    // ---------- 创建知识库 ----------
    async function create(data: CreateKBRequest) {
        const res = await kbApi.createKB(data)
        const newKB = res.data.data
        list.value.unshift(newKB) // 新建的放最前面
        ElMessage.success('知识库创建成功')
        return newKB
    }

    // ---------- 更新知识库 ----------
    async function update(id: number, data: UpdateKBRequest) {
        const res = await kbApi.updateKB(id, data)
        const updated = res.data.data
        // 同步更新列表中的数据
        const idx = list.value.findIndex(kb => kb.id === id)
        if (idx !== -1) list.value[idx] = updated
        // 如果当前详情页正是这个知识库，也更新
        if (current.value?.id === id) current.value = updated
        ElMessage.success('知识库更新成功')
        return updated
    }

    // ---------- 删除知识库 ----------
    async function remove(id: number) {
        await kbApi.deleteKB(id)
        list.value = list.value.filter(kb => kb.id !== id)
        if (current.value?.id === id) current.value = null
        ElMessage.success('知识库已删除')
    }

    // ---------- 加载统计信息 ----------
    async function fetchStatistics(id: number) {
        try {
            const res = await kbApi.getKBStatistics(id)
            statistics.value = res.data.data
            return res.data.data
        } catch {
            statistics.value = null
        }
    }

    // ---------- 基础 setter ----------
    function setCurrent(kb: KnowledgeBaseDTO | null) { current.value = kb }

    return {
        list, current, statistics, loading,
        fetchList, fetchById, create, update, remove, fetchStatistics, setCurrent,
    }
})
