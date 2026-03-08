export interface DocumentInfo {
    id: number
    kbId: number
    uploaderId: number
    title: string
    fileType: string
    status: DocumentStatus
    chunkCount: number
    createdAt: string
    updatedAt: string
}

export type DocumentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'

export interface DocumentUploadResponse {
    documentId: number
    taskId: string
    fileName: string
    fileType: string
    status: string
}
