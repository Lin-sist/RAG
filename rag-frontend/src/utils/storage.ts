const TOKEN_PREFIX = 'rag_'

export function setToken(key: string, value: string): void {
    localStorage.setItem(TOKEN_PREFIX + key, value)
}

export function getToken(key: string): string | null {
    return localStorage.getItem(TOKEN_PREFIX + key)
}

export function removeToken(key: string): void {
    localStorage.removeItem(TOKEN_PREFIX + key)
}

export function clearTokens(): void {
    removeToken('accessToken')
    removeToken('refreshToken')
}
