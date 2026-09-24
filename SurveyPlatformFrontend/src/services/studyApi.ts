import api from './api'

export type CreateStudyRequest = {
  title: string
  description?: string
  templateCode: string
}

export type StudyResponse = {
  id: string
  title: string
  description: string | null
  status: 'DRAFT' | 'COLLECTING' | 'CLOSED'
  createdAt: string
  updatedAt: string
}

export async function createStudy(request: CreateStudyRequest) {
  const response = await api.post<StudyResponse>('/api/studies', request)
  return response.data
}
