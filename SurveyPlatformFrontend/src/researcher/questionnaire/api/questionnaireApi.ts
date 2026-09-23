// 真实接口对应
// com.cs_42_3.surveyplatformbackend.questionnaire.api.QuestionnaireController
// （/api/studies/{studyId}/questionnaire，FR-36~39）。等该 Controller
// 可用后，把 shared/api/mockConfig.ts 里的 USE_MOCK_API 改成 false 即可。
import api from '../../../services/api'
import { USE_MOCK_API } from '../../../shared/api/mockConfig'
import type {
  QuestionnaireResponse,
  SaveQuestionnaireRequest,
} from '../../../shared/types/questionnaire'
import { mockGetQuestionnaire, mockSaveQuestionnaire } from './mockQuestionnaire'

export async function getQuestionnaire(studyId: string): Promise<QuestionnaireResponse> {
  if (USE_MOCK_API) {
    return mockGetQuestionnaire(studyId)
  }
  const response = await api.get<QuestionnaireResponse>(
    `/api/studies/${studyId}/questionnaire`,
  )
  return response.data
}

export async function saveQuestionnaire(
  studyId: string,
  request: SaveQuestionnaireRequest,
): Promise<QuestionnaireResponse> {
  if (USE_MOCK_API) {
    return mockSaveQuestionnaire(studyId, request)
  }
  const response = await api.put<QuestionnaireResponse>(
    `/api/studies/${studyId}/questionnaire`,
    request,
  )
  return response.data
}
