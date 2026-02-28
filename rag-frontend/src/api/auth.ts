import request from './request'
import type { ApiResponse } from '@/types/api'
import type { LoginRequest, AuthResponse } from '@/types/auth'

export function login(data: LoginRequest) {
    return request.post<ApiResponse<AuthResponse>>('/auth/login', data)
}

export function logout() {
    return request.post<ApiResponse<void>>('/auth/logout')
}

export function refreshToken(token: string) {
    return request.post<ApiResponse<AuthResponse>>('/auth/refresh', { refreshToken: token })
}
