import request from './request'
import type { ApiResponse } from '@/types/api'

export interface Profile {
    id: number
    username: string
    displayName: string | null
    email: string | null
    hasAvatar: boolean
    revision: number
}
export type AvatarAction = 'KEEP' | 'REPLACE' | 'REMOVE'
export interface ProfileWrite {
    displayName: string | null
    email: string | null
    expectedRevision: number
    avatarAction: AvatarAction
}
function validate(value: Profile): Profile {
    if (!value || !Number.isSafeInteger(value.id) || value.id <= 0 || typeof value.username !== 'string'
        || !value.username.trim() || (value.displayName !== null && typeof value.displayName !== 'string')
        || (value.email !== null && typeof value.email !== 'string') || typeof value.hasAvatar !== 'boolean'
        || !Number.isSafeInteger(value.revision) || value.revision < 0) throw new Error('Invalid profile response')
    return value
}
export async function getProfile(): Promise<Profile> {
    return validate((await request.get<ApiResponse<Profile>>('/api/account/profile')).data.data)
}
export async function putProfile(value: ProfileWrite, avatar?: Blob): Promise<Profile> {
    const body = new FormData()
    body.append('profile', new Blob([JSON.stringify(value)], { type: 'application/json' }))
    if (avatar) body.append('avatar', avatar, 'avatar.png')
    return validate((await request.put<ApiResponse<Profile>>('/api/account/profile', body,
        { headers: { 'Content-Type': undefined }, noAuthReplay: true })).data.data)
}
export async function getAvatar(): Promise<Blob> {
    const response = await request.get<Blob>('/api/account/profile/avatar', { responseType: 'blob' })
    if (response.data.type !== 'image/png' || response.data.size > 512 * 1024) throw new Error('Invalid avatar response')
    return response.data
}
