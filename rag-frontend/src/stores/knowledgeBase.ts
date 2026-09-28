import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { KnowledgeBaseDTO, CreateKBRequest, UpdateKBRequest, KnowledgeBaseStatistics } from '@/types/knowledgeBase'
import { normalizeError } from '@/api/errors'
import * as kbApi from '@/api/knowledgeBase'

export const useKnowledgeBaseStore = defineStore('knowledgeBase', () => {
    // ---------- 状态 ----------
    const list = ref<KnowledgeBaseDTO[]>([])
    const current = ref<KnowledgeBaseDTO | null>(null)
    const statistics = ref<KnowledgeBaseStatistics | null>(null)
    const listLoading = ref(false)
    const detailLoading = ref(false)
    const loading = computed(() => listLoading.value || detailLoading.value)
    const listError = ref('')
    const detailError = ref('')
    const statisticsError = ref('')
    let listSequence = 0, detailSequence = 0, statsSequence = 0

    // ---------- 加载知识库列表 ----------
    async function fetchList() {
        const seq = ++listSequence
        listLoading.value = true
        listError.value = ''
        try {
            const res = await kbApi.listKB()
            if (seq === listSequence) list.value = res.data.data
        } catch (error) {
            if (seq === listSequence) listError.value = normalizeError(error).message
            throw error
        } finally {
            if (seq === listSequence) listLoading.value = false
        }
    }

    async function fetchById(id: number) {
        const seq = ++detailSequence
        detailLoading.value = true
        detailError.value = ''
        current.value = null
        try {
            const res = await kbApi.getKBById(id)
            if (seq === detailSequence) current.value = res.data.data
            return res.data.data
        } catch (error) {
            if (seq === detailSequence) detailError.value = normalizeError(error).message
            throw error
        } finally {
            if (seq === detailSequence) detailLoading.value = false
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
        const seq = ++statsSequence
        statistics.value = null
        statisticsError.value = ''
        try {
            const res = await kbApi.getKBStatistics(id)
            if (seq === statsSequence) statistics.value = res.data.data
            return res.data.data
        } catch (error) {
            if (seq === statsSequence) statisticsError.value = normalizeError(error).message
        }
    }

    // ---------- 基础 setter ----------
    function setCurrent(kb: KnowledgeBaseDTO | null) { current.value = kb }

    return {
        list, current, statistics, loading, listLoading, detailLoading, listError, detailError, statisticsError,
        fetchList, fetchById, create, update, remove, fetchStatistics, setCurrent,
    }
})
