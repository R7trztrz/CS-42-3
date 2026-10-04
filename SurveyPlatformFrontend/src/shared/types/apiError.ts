// The M4 Contract v1 error envelope (SurveyPlatformBackend/docs/m4-api-contract-v1.md
// section 4). Every M4 business/validation error uses this one shape; the frontend
// must branch on `code`, never on the English `message` (section 4's own rule).
export interface ApiErrorDetail {
  field: string
  index: number | null
  itemId: string | null
  ruleIndex: number | null
  code: string
  message: string
}

export interface ApiErrorResponse {
  code: string
  message: string
  timestamp: string
  path: string
  details: ApiErrorDetail[]
}

// Narrows an unknown caught error down to the backend's error envelope, when it
// is one. Use this instead of reading `error.response.data` ad hoc so every
// call site gets the same shape check.
export function asApiError(error: unknown): ApiErrorResponse | null {
  if (
    typeof error === 'object' &&
    error !== null &&
    'response' in error &&
    typeof (error as { response?: unknown }).response === 'object'
  ) {
    const response = (error as { response?: { data?: unknown } }).response
    const data = response?.data
    if (
      typeof data === 'object' &&
      data !== null &&
      'code' in data &&
      typeof (data as { code?: unknown }).code === 'string'
    ) {
      return data as ApiErrorResponse
    }
  }
  return null
}

// Best-effort human-readable fallback for places that just need one line of
// text (e.g. a toast). Prefer `asApiError(...).code` for anything that needs
// to behave differently per error type.
export function describeError(error: unknown, fallback: string): string {
  const apiError = asApiError(error)
  if (apiError) {
    return apiError.message || fallback
  }
  return error instanceof Error ? error.message : fallback
}
