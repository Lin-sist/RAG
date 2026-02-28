export interface KnowledgeBaseDTO {
    id: number
    name: string
    description: string
    ownerId: number
    vectorCollection: string
    documentCount: number
    isPublic: boolean
    createdAt: string
    updatedAt: string
}

export interface CreateKBRequest {
    name: string
    description?: string
    isPublic?: boolean
}

export interface UpdateKBRequest {
    name?: string
    description?: string
    isPublic?: boolean
}

export interface KnowledgeBaseStatistics {
    kbId: number
    documentCount: number
    vectorCount: number
    queryCount: number
}
