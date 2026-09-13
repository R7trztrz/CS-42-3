import type {
    ApiErrorResponse,
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