import api from './api'

export type StudyStatus = 'DRAFT' | 'COLLECTING' | 'CLOSED'

export type CreateStudyRequest = {
  title: string
  description?: string
  templateCode: string
}

export type StudyResponse = {
  id: string
  title: string
  description: string | null
  status: StudyStatus
  createdAt: string
  updatedAt: string
  version: number
  eyeTrackingEnabled: boolean
  questionnaireEnabled: boolean
}

export type StudySummaryResponse = {
  id: string
  title: string
  status: StudyStatus
  createdAt: string
}

export type StudyPageResponse = {
  content: StudySummaryResponse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function createStudy(request: CreateStudyRequest) {
  const response = await api.post<StudyResponse>('/api/studies', request)
  return response.data
}

export async function listStudies(page = 0, size = 20) {
  const response = await api.get<StudyPageResponse>('/api/studies', {
    params: { page, size },
  })

  return response.data
}
