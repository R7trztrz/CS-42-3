export type ResearcherRole = 'RESEARCHER'

export interface RegisterRequest {
    email: string
    password: string
    confirmPassword: string
    captchaToken: string
}

export interface RegisterResponse {
    id: string
    email: string
    role: ResearcherRole
}

export interface LoginRequest {
    email: string
    password: string
}

export interface LoginResponse {
    token: string
    tokenType: string
    expiresIn: number
}

export interface ApiErrorResponse {
    error?: string
}
