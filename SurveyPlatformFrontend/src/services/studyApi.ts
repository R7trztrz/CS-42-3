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
  publishedAt: string | null
  participationUrl: string | null
}

export type StudySummaryResponse = {
  id: string
  title: string
  status: StudyStatus
  createdAt: string
  updatedAt: string
}

export type StudyFeedResponse = {
  studyId: string
  templateCode: string
  theme: string | null
  content: Record<string, unknown> | null
  schemaVersion: number | null
  version: number
  updatedAt: string
}

export type ParticipationResponse = {
  title: string
  description: string | null
  eyeTrackingEnabled: boolean
  questionnaireEnabled: boolean
  theme: string | null
  content: Record<string, unknown>
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

export async function getStudy(studyId: string) {
  const response = await api.get<StudyResponse>(`/api/studies/${studyId}`)
  return response.data
}

export async function getStudyFeed(studyId: string) {
  const response = await api.get<StudyFeedResponse>(`/api/studies/${studyId}/feed`)
  return response.data
}

export async function saveStudyFeed(
  studyId: string,
  content: Record<string, unknown>,
  version: number,
) {
  const response = await api.put<StudyFeedResponse>(`/api/studies/${studyId}/feed`, {
    content,
    version,
  })
  return response.data
}

export async function publishStudy(studyId: string, version: number) {
  const response = await api.post<StudyResponse>(`/api/studies/${studyId}/publish`, {
    version,
  })
  return response.data
}

export async function getParticipation(token: string) {
  const response = await api.get<ParticipationResponse>(`/api/participation/${token}`)
  return response.data
}
