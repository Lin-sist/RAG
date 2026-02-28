import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { KnowledgeBaseDTO } from '@/types/knowledgeBase'

export const useKnowledgeBaseStore = defineStore('knowledgeBase', () => {
    const list = ref<KnowledgeBaseDTO[]>([])
    const current = ref<KnowledgeBaseDTO | null>(null)
    const loading = ref(false)

    function setList(kbs: KnowledgeBaseDTO[]) { list.value = kbs }
    function setCurrent(kb: KnowledgeBaseDTO | null) { current.value = kb }
    function setLoading(val: boolean) { loading.value = val }

    return { list, current, loading, setList, setCurrent, setLoading }
})
