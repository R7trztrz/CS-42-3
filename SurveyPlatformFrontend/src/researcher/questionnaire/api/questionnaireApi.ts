// Real endpoints per SurveyPlatformBackend/docs/m4-api-contract-v1.md
// section 6, frozen at origin/JustinLiu@fa393d8:
// com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.QuestionnaireController.
import { isAxiosError } from 'axios'
import api from '../../../services/api'
import { asApiError } from '../../../shared/types/apiError'
import type {
  QuestionnaireResponse,
  SaveQuestionnaireRequest,
} from '../../../shared/types/questionnaire'

// Contract 6.1: an owned DRAFT study with no saved questionnaire returns
// 404 QUESTIONNAIRE_NOT_FOUND. That one specific code (and only that code)
// means "no draft yet" and should become an empty, unsaved editor - every
// other 404 (e.g. the study itself doesn't exist or isn't owned) must stay
// an error. Returning null here is the signal for the former case.
export async function getQuestionnaire(
  studyId: string,
): Promise<QuestionnaireResponse | null> {
  try {
    const response = await api.get<QuestionnaireResponse>(
      `/api/studies/${studyId}/questionnaire`,
    )
    return response.data
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 404) {
      const apiError = asApiError(error)
      if (apiError?.code === 'QUESTIONNAIRE_NOT_FOUND') {
        return null
      }
    }
    throw error
  }
}

export async function saveQuestionnaire(
  studyId: string,
  request: SaveQuestionnaireRequest,
): Promise<QuestionnaireResponse> {
  const response = await api.put<QuestionnaireResponse>(
    `/api/studies/${studyId}/questionnaire`,
    request,
  )
  return response.data
}
