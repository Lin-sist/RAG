export interface KnowledgeBaseDTO {
    id: number
    name: string
    description: string
    ownerId: number
    vectorCollection: string
    vectorProviderFamily?: string | null
    vectorModel?: string | null
    vectorEndpointIdentity?: string | null
    vectorRequestContract?: string | null
    vectorDimension?: number | null
    vectorGeneration?: string | null
    vectorIdentityFingerprint?: string | null
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
    vectorProviderFamily?: string | null
    vectorModel?: string | null
    vectorEndpointIdentity?: string | null
    vectorRequestContract?: string | null
    vectorDimension?: number | null
    vectorGeneration?: string | null
    vectorIdentityFingerprint?: string | null
    documentCount: number
    vectorCount: number
    queryCount: number
}
