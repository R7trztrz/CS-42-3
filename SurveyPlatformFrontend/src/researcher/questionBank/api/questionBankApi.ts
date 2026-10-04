// Real endpoints per SurveyPlatformBackend/docs/m4-api-contract-v1.md
// section 5, frozen at origin/JustinLiu@fa393d8:
// com.cs_42_3.surveyplatformbackend.survey.api.QuestionController.
import api from '../../../services/api'
import type {
  CreateQuestionRequest,
  ListQuestionsParams,
  QuestionFormValues,
  QuestionPageResponse,
  QuestionResponse,
  UpdateQuestionRequest,
} from '../../../shared/types/question'

export async function listQuestions(
  params: ListQuestionsParams = {},
): Promise<QuestionPageResponse> {
  const response = await api.get<QuestionPageResponse>('/api/questions', {
    params: {
      page: params.page,
      size: params.size,
      type: params.type,
      // Contract 5.1: the query parameter is `search`, not the old `keyword`.
      search: params.search || undefined,
    },
  })
  return response.data
}

export async function getQuestion(questionId: string): Promise<QuestionResponse> {
  const response = await api.get<QuestionResponse>(`/api/questions/${questionId}`)
  return response.data
}

// Builds the type-specific fields shared by create and update requests.
// Contract 5.3 table: choice types need >=2 options and null scale fields;
// SCALE needs scale bounds and an empty options array; TEXT needs both empty/null.
function buildTypeFields(values: QuestionFormValues) {
  const isChoice = values.type === 'SINGLE_CHOICE' || values.type === 'MULTI_CHOICE'
  const isScale = values.type === 'SCALE'
  return {
    scaleMin: isScale ? values.scaleMin : null,
    scaleMax: isScale ? values.scaleMax : null,
    scaleMinLabel: isScale ? values.scaleMinLabel : null,
    scaleMaxLabel: isScale ? values.scaleMaxLabel : null,
    options: isChoice ? values.options : [],
  }
}

export async function createQuestion(
  values: QuestionFormValues,
): Promise<QuestionResponse> {
  const typeFields = buildTypeFields(values)
  const request: CreateQuestionRequest = {
    type: values.type,
    questionText: values.questionText,
    required: values.required,
    options: typeFields.options.map((option) => ({ optionText: option.optionText })),
    scaleMin: typeFields.scaleMin,
    scaleMax: typeFields.scaleMax,
    scaleMinLabel: typeFields.scaleMinLabel,
    scaleMaxLabel: typeFields.scaleMaxLabel,
  }
  const response = await api.post<QuestionResponse>('/api/questions', request)
  return response.data
}

export async function updateQuestion(
  questionId: string,
  values: QuestionFormValues,
): Promise<QuestionResponse> {
  const typeFields = buildTypeFields(values)
  // Contract 5.4: required whenever the submitted options retain none of
  // the question's existing optionIds - including when there are no
  // options at all (SCALE/TEXT), where it is a harmless no-op.
  const replaceAllOptions = typeFields.options.every((option) => option.optionId === null)
  const request: UpdateQuestionRequest = {
    type: values.type,
    questionText: values.questionText,
    required: values.required,
    options: typeFields.options.map((option) => ({
      optionId: option.optionId,
      optionText: option.optionText,
    })),
    scaleMin: typeFields.scaleMin,
    scaleMax: typeFields.scaleMax,
    scaleMinLabel: typeFields.scaleMinLabel,
    scaleMaxLabel: typeFields.scaleMaxLabel,
    replaceAllOptions,
  }
  const response = await api.put<QuestionResponse>(`/api/questions/${questionId}`, request)
  return response.data
}

export async function deleteQuestion(questionId: string): Promise<void> {
  await api.delete(`/api/questions/${questionId}`)
}
