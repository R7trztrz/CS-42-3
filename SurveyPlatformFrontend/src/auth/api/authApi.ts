import type {
    ApiErrorResponse,
    LoginRequest,
    LoginResponse,
    RegisterRequest,
    RegisterResponse,
} from '../types/auth'

const API_BASE_URL = 'http://localhost:8080'

export async function registerResearcher(
    request: RegisterRequest,
): Promise<RegisterResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
    })

    if (!response.ok) {
        const errorBody = (await response.json()) as ApiErrorResponse

        throw new Error(
            errorBody.error || 'Registration failed',
        )
    }

    return (await response.json()) as RegisterResponse
}
export async function loginResearcher(
    request: LoginRequest,
): Promise<LoginResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
    })

    if (!response.ok) {
        const errorBody = (await response.json()) as ApiErrorResponse

        throw new Error(
            errorBody.error || 'Login failed',
        )
    }

    return (await response.json()) as LoginResponse
}