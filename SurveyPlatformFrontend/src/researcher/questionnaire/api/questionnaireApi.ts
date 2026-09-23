// Real endpoints match
// com.cs_42_3.surveyplatformbackend.questionnaire.api.QuestionnaireController
// (/api/studies/{studyId}/questionnaire, FR-36~39). Swap USE_MOCK_API to
// false in shared/api/mockConfig.ts once that controller is reachable.
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
