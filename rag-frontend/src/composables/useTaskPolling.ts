import { onUnmounted } from 'vue'
import { getTaskStatus } from '@/api/task'
import { createTaskPoller } from '@/utils/taskPoller'

// 每个页面唯一轮询管理器，可管理多个上传任务；请求完成后才安排下一次。
export function useTaskPolling() {
    const poller = createTaskPoller(async id => (await getTaskStatus(id)).data.data)
    onUnmounted(poller.stopAll)
    return poller
}
