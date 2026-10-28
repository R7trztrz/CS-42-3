import api from './api'

export type ExportDataType = 'QUESTIONNAIRE' | 'GAZE' | 'BEHAVIOR'
export type ExportJob = {
  id: string
  studyId: string
  sessionId: string | null
  scope: 'STUDY' | 'SESSION'
  dataTypes: ExportDataType[]
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'EXPIRED'
  createdAt: string
  startedAt: string | null
  snapshotAt: string | null
  completedAt: string | null
  expiresAt: string | null
  errorCode: string | null
  errorMessage: string | null
  failedSessionId: string | null
}

export async function createStudyExport(studyId: string, dataTypes: ExportDataType[]) {
  return (await api.post<ExportJob>(`/api/studies/${studyId}/exports`, { dataTypes })).data
}

export async function createSessionExport(studyId: string, sessionId: string, dataTypes: ExportDataType[]) {
  return (await api.post<ExportJob>(`/api/studies/${studyId}/participants/${sessionId}/exports`, { dataTypes })).data
}

export async function getStudyExport(studyId: string, exportId: string, signal?: AbortSignal) {
  return (await api.get<ExportJob>(`/api/studies/${studyId}/exports/${exportId}`, { signal })).data
}

export async function downloadStudyExport(studyId: string, exportId: string, scope: ExportJob['scope'] = 'STUDY') {
  const response = await api.get<Blob>(`/api/studies/${studyId}/exports/${exportId}/download`, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = `${scope === 'SESSION' ? 'session' : 'study'}-export-${exportId}.zip`
  document.body.appendChild(link)
  try { link.click() }
  finally { link.remove(); window.setTimeout(() => URL.revokeObjectURL(url), 60_000) }
}
