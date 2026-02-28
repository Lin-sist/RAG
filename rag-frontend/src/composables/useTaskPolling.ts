import { ref, onUnmounted } from 'vue'
import { getTaskStatus } from '@/api/task'
import type { TaskStatusResponse } from '@/types/task'

export function useTaskPolling() {
    const taskStatus = ref<TaskStatusResponse | null>(null)
    const isPolling = ref(false)
    const error = ref<string | null>(null)
    let timer: ReturnType<typeof setInterval> | null = null

    function startPolling(
        taskId: string,
        interval = 2000,
        onComplete?: (status: TaskStatusResponse) => void,
        onFailed?: (status: TaskStatusResponse) => void
    ) {
        stopPolling()
        isPolling.value = true
        error.value = null

        const poll = async () => {
            try {
                const res = await getTaskStatus(taskId)
                taskStatus.value = res.data.data
                const state = res.data.data.state
                if (state === 'COMPLETED') { stopPolling(); onComplete?.(res.data.data) }
                else if (state === 'FAILED' || state === 'CANCELLED') { stopPolling(); onFailed?.(res.data.data) }
            } catch (e) {
                error.value = e instanceof Error ? e.message : '轮询失败'
                stopPolling()
            }
        }

        poll()
        timer = setInterval(poll, interval)
    }

    function stopPolling() {
        if (timer) { clearInterval(timer); timer = null }
        isPolling.value = false
    }

    onUnmounted(() => stopPolling())

    return { taskStatus, isPolling, error, startPolling, stopPolling }
}
