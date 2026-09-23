// Mirrors the backend DTOs in
// com.cs_42_3.surveyplatformbackend.questionnaire.api.dto (FR-36~39).
// See SurveyPlatformBackend/docs/questionnaire-module.md for the design.

import type { QuestionResponse } from './question'

export interface QuestionnaireBranchRuleRequest {
  sourceOptionId: string | null
  sourceScaleValue: number | null
  targetPosition: number
}

export interface QuestionnaireItemRequest {
  questionId: string
  branchRules: QuestionnaireBranchRuleRequest[]
}

export interface SaveQuestionnaireRequest {
  items: QuestionnaireItemRequest[]
}

export interface QuestionnaireBranchRuleResponse {
  id: string
  sourceOptionId: string | null
  sourceScaleValue: number | null
  targetItemId: string
  targetPosition: number
}

export interface QuestionnaireItemResponse {
  id: string
  position: number
  question: QuestionResponse
  branchRules: QuestionnaireBranchRuleResponse[]
}

export interface QuestionnaireResponse {
  id: string | null
  studyId: string
  items: QuestionnaireItemResponse[]
  createdAt: string | null
  updatedAt: string | null
}

// Editor-local working copy. The request/response shapes reference a branch
// rule's target by array position, which breaks the moment the researcher
// reorders items; the editor instead keeps a stable per-row clientId and
// only resolves target positions right before calling saveQuestionnaire.
export interface QuestionnaireEditorBranchRule {
  sourceOptionId: string | null
  sourceScaleValue: number | null
  targetClientId: string
}

export interface QuestionnaireEditorItem {
  clientId: string
  question: QuestionResponse
  branchRules: QuestionnaireEditorBranchRule[]
}
