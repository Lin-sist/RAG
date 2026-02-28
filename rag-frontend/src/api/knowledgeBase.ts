import request from './request'
import type { ApiResponse } from '@/types/api'
import type { KnowledgeBaseDTO, CreateKBRequest, UpdateKBRequest, KnowledgeBaseStatistics } from '@/types/knowledgeBase'
import type { DocumentInfo, DocumentUploadResponse } from '@/types/document'

export function createKB(data: CreateKBRequest) {
    return request.post<ApiResponse<KnowledgeBaseDTO>>('/api/knowledge-bases', data)
}

export function listKB() {
    return request.get<ApiResponse<KnowledgeBaseDTO[]>>('/api/knowledge-bases')
}

export function getKBById(id: number) {
    return request.get<ApiResponse<KnowledgeBaseDTO>>(`/api/knowledge-bases/${id}`)
}

export function updateKB(id: number, data: UpdateKBRequest) {
    return request.put<ApiResponse<KnowledgeBaseDTO>>(`/api/knowledge-bases/${id}`, data)
}

export function deleteKB(id: number) {
    return request.delete<ApiResponse<void>>(`/api/knowledge-bases/${id}`)
}

export function getKBStatistics(id: number) {
    return request.get<ApiResponse<KnowledgeBaseStatistics>>(`/api/knowledge-bases/${id}/statistics`)
}

export function uploadDocument(kbId: number, file: File, title?: string) {
    const formData = new FormData()
    formData.append('file', file)
    if (title) formData.append('title', title)
    return request.post<ApiResponse<DocumentUploadResponse>>(
        `/api/knowledge-bases/${kbId}/documents`, formData,
        { headers: { 'Content-Type': 'multipart/form-data' } }
    )
}

export function listDocuments(kbId: number) {
    return request.get<ApiResponse<DocumentInfo[]>>(`/api/knowledge-bases/${kbId}/documents`)
}

export function deleteDocument(kbId: number, docId: number) {
    return request.delete<ApiResponse<void>>(`/api/knowledge-bases/${kbId}/documents/${docId}`)
}
