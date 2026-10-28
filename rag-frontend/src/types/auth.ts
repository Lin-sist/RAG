export interface LoginRequest {
    username: string
    password: string
}

export interface RefreshTokenRequest {
    refreshToken: string
}

export interface UserInfo {
    id: number
    username: string
    email: string
    displayName?: string | null
    hasAvatar?: boolean
    profileRevision?: number
}

export interface AuthResponse {
    accessToken: string
    refreshToken: string
    expiresIn: number
    tokenType: string
    userInfo: UserInfo
}
