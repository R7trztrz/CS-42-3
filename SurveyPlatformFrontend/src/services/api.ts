import axios from 'axios'
import type { ApiErrorResponse } from '../auth/types/auth'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('researcherToken')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

api.interceptors.response.use(
    (response) => response,

    (error) => {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        const errorCode = error.response?.data?.code

        if (errorCode === 'AUTH_UNAUTHORIZED') {
          localStorage.removeItem('researcherToken')
          localStorage.removeItem('researcherTokenType')

          if (window.location.pathname !== '/login') {
            window.location.href = '/login'
          }
        }
      }

      return Promise.reject(error)
    },
)

export default api