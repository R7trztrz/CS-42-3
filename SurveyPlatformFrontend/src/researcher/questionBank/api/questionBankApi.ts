// Real endpoints match
// com.cs_42_3.surveyplatformbackend.survey.api.QuestionController
// (/api/questions, FR-32~35). Swap USE_MOCK_API to false in
// shared/api/mockConfig.ts once that controller is reachable; the function
// signatures below won't need to change.
import api from '../../../services/api'
import { USE_MOCK_API } from '../../../shared/api/mockConfig'
import type {
  QuestionFormValues,
  QuestionResponse,
  QuestionSummaryResponse,
  QuestionType,
} from '../../../shared/types/question'
import {
  mockCreateQuestion,
  mockDeleteQuestion,
  mockGetQuestion,
  mockListQuestions,
  mockUpdateQuestion,
} from './mockQuestionBank'

export async function listQuestions(
  type: QuestionType | '' = '',
  keyword = '',
): Promise<QuestionSummaryResponse[]> {
  if (USE_MOCK_API) {
    return mockListQuestions(type, keyword)
  }
  const response = await api.get<QuestionSummaryResponse[]>('/api/questions', {
    params: { type: type || undefined, keyword: keyword || undefined },
  })
  return response.data
}

export async function getQuestion(id: string): Promise<QuestionResponse> {
  if (USE_MOCK_API) {
    return mockGetQuestion(id)
  }
  const response = await api.get<QuestionResponse>(`/api/questions/${id}`)
  return response.data
}

export async function createQuestion(
  values: QuestionFormValues,
): Promise<QuestionResponse> {
  if (USE_MOCK_API) {
    return mockCreateQuestion(values)
  }
  const response = await api.post<QuestionResponse>('/api/questions', values)
  return response.data
}

export async function updateQuestion(
  id: string,
  values: QuestionFormValues,
): Promise<QuestionResponse> {
  if (USE_MOCK_API) {
    return mockUpdateQuestion(id, values)
  }
  const response = await api.put<QuestionResponse>(`/api/questions/${id}`, values)
  return response.data
}

export async function deleteQuestion(id: string): Promise<void> {
  if (USE_MOCK_API) {
    return mockDeleteQuestion(id)
  }
  await api.delete(`/api/questions/${id}`)
}
