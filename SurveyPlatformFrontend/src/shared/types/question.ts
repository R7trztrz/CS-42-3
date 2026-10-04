// Mirrors SurveyPlatformBackend/docs/m4-api-contract-v1.md section 5
// (question-bank API), frozen at origin/JustinLiu@fa393d8. Field names
// (questionId/optionId, not id) and the pagination envelope are the
// contract's, not a frontend convention - keep this in sync with the
// backend DTOs if the contract moves past v1.

export type QuestionType = 'SINGLE_CHOICE' | 'MULTI_CHOICE' | 'SCALE' | 'TEXT'

// Centralizes the display label per question type so pages don't each
// define their own copy and drift out of sync.
export const QUESTION_TYPE_LABELS: Record<QuestionType, string> = {
  SINGLE_CHOICE: 'Single choice',
  MULTI_CHOICE: 'Multiple choice',
  SCALE: 'Scale',
  TEXT: 'Text',
}

export interface QuestionOptionResponse {
  optionId: string
  optionText: string
  optionOrder: number
}

export interface QuestionResponse {
  questionId: string
  type: QuestionType
  questionText: string
  required: boolean
  options: QuestionOptionResponse[]
  scaleMin: number | null
  scaleMax: number | null
  scaleMinLabel: string | null
  scaleMaxLabel: string | null
  createdAt: string
  updatedAt: string
}

export interface QuestionSummaryResponse {
  questionId: string
  type: QuestionType
  questionText: string
  updatedAt: string
}

// Contract 5.1: GET /api/questions returns this page envelope, not a bare
// array and not Spring's own Page internals.
export interface QuestionPageResponse {
  content: QuestionSummaryResponse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ListQuestionsParams {
  page?: number
  size?: number
  type?: QuestionType
  search?: string
}

// Contract 5.3: create requests must omit optionId (or send it null);
// identities are server-generated.
export interface CreateQuestionOptionRequest {
  optionText: string
}

export interface CreateQuestionRequest {
  type: QuestionType
  questionText: string
  required: boolean
  options: CreateQuestionOptionRequest[]
  scaleMin: number | null
  scaleMax: number | null
  scaleMinLabel: string | null
  scaleMaxLabel: string | null
}

// Contract 5.4: update requests carry each option's existing optionId (or
// null to add a new one) so the backend can tell a rename apart from a
// replace. `replaceAllOptions` is required when nothing in the submitted
// list keeps an existing identity.
export interface UpdateQuestionOptionRequest {
  optionId: string | null
  optionText: string
}

export interface UpdateQuestionRequest {
  type: QuestionType
  questionText: string
  required: boolean
  options: UpdateQuestionOptionRequest[]
  scaleMin: number | null
  scaleMax: number | null
  scaleMinLabel: string | null
  scaleMaxLabel: string | null
  replaceAllOptions: boolean
}

// Frontend-only editor state for the create/edit form. Each option row
// keeps the optionId it was loaded with (null for a freshly added row) so
// questionBankApi can build an UpdateQuestionRequest without the page
// having to reconstruct option identity itself.
export interface QuestionOptionFormValue {
  optionId: string | null
  optionText: string
}

export interface QuestionFormValues {
  type: QuestionType
  questionText: string
  required: boolean
  options: QuestionOptionFormValue[]
  scaleMin: number | null
  scaleMax: number | null
  scaleMinLabel: string | null
  scaleMaxLabel: string | null
}
