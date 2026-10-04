// Mirrors SurveyPlatformBackend/docs/m4-api-contract-v1.md sections 6
// (questionnaire API) and 7 (FR38 branch contract), frozen at
// origin/JustinLiu@fa393d8.

import type { QuestionResponse } from './question'

export type QuestionnaireContentSource = 'LIVE_DRAFT' | 'PUBLISHED_SNAPSHOT'
export type QuestionnaireItemReferenceStatus = 'VALID' | 'MISSING_QUESTION'

export interface QuestionnaireBranchRuleResponse {
  id: string
  sourceOptionId: string | null
  sourceScaleValue: number | null
  targetItemId: string
  targetPosition: number
}

export interface QuestionnaireItemResponse {
  itemId: string
  position: number
  missing: boolean
  // Null exactly when `missing` is true (contract 5.5): the bank question
  // this item pointed to was deleted and the draft hasn't been repaired yet.
  question: QuestionResponse | null
  referenceStatus: QuestionnaireItemReferenceStatus
  branchRules: QuestionnaireBranchRuleResponse[]
  // The implicit "no rule matched" target (contract 7.3); null on the last
  // item, where the default transition is END.
  defaultNextItemId: string | null
}

export interface QuestionnaireValidationIssue {
  code: string
  itemIndex: number | null
  itemId: string | null
  ruleIndex: number | null
  message: string
}

export interface QuestionnaireResponse {
  questionnaireId: string
  // Non-null only once contentSource is PUBLISHED_SNAPSHOT.
  snapshotId: string | null
  studyId: string
  contentSource: QuestionnaireContentSource
  items: QuestionnaireItemResponse[]
  // The questionnaire's own optimistic-lock version - distinct from the
  // Study's version used by the publish endpoint (contract 8).
  version: number
  updatedAt: string
  publishedAt: string | null
  valid: boolean
  validationIssues: QuestionnaireValidationIssue[]
}

// Contract 6.2: save requests send the entire desired final questionnaire,
// never a delta, and must preserve each retained item's itemId.
export interface QuestionnaireBranchRuleRequest {
  sourceOptionId: string | null
  sourceScaleValue: number | null
  targetPosition: number
}

export interface QuestionnaireItemRequest {
  // Null adds a new item; otherwise must be a stable itemId from the last
  // successful GET/PUT.
  itemId: string | null
  questionId: string
  branchRules: QuestionnaireBranchRuleRequest[]
}

export interface SaveQuestionnaireRequest {
  // Always include this key. Null only for the first save (or its exact
  // replay); every later save uses the `version` from the latest
  // successful GET/PUT response.
  expectedVersion: number | null
  items: QuestionnaireItemRequest[]
}

// Editor-local working copy. The request/response shapes reference a branch
// rule's target by array position, which breaks the moment the researcher
// reorders items; the editor instead keeps a stable per-row clientId and
// only resolves target positions right before calling saveQuestionnaire.
// `itemId` is carried alongside `clientId` (and preserved through reorders)
// because the backend contract requires retaining it across saves.
export interface QuestionnaireEditorBranchRule {
  sourceOptionId: string | null
  sourceScaleValue: number | null
  targetClientId: string
}

export interface QuestionnaireEditorItem {
  clientId: string
  itemId: string | null
  questionId: string
  // Null when `missing` is true; the editor still renders the row (as a
  // "missing reference" placeholder) so the researcher can remove it.
  question: QuestionResponse | null
  missing: boolean
  branchRules: QuestionnaireEditorBranchRule[]
}
