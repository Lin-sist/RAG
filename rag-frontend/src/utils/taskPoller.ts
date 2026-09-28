import type { TaskStatusResponse } from '../types/task'

interface Observer {
    update(status: TaskStatusResponse): void
    error(error: unknown): void
    terminal?(status: TaskStatusResponse): void
}
export function createTaskPoller(fetchStatus: (id: string) => Promise<TaskStatusResponse>, delay = 2000) {
    const jobs = new Map<string, { timer?: ReturnType<typeof setTimeout> }>()
    function stop(id: string) {
        const job = jobs.get(id)
        if (job?.timer) clearTimeout(job.timer)
        jobs.delete(id)
    }
    function stopAll() { for (const id of jobs.keys()) stop(id) }
    function start(id: string, observer: Observer) {
        if (jobs.has(id)) return
        const job: { timer?: ReturnType<typeof setTimeout> } = {}
        jobs.set(id, job)
        async function poll() {
            try {
                const status = await fetchStatus(id)
                if (jobs.get(id) !== job) return
                observer.update(status)
                if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(status.state)) {
                    stop(id)
                    observer.terminal?.(status)
                } else {
                    job.timer = setTimeout(poll, delay)
                }
            } catch (error) {
                if (jobs.get(id) !== job) return
                stop(id)
                observer.error(error)
            }
        }
        void poll()
    }
    return { start, stop, stopAll }
}
