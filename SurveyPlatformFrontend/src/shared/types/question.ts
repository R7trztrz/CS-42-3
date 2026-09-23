// Mirrors the backend DTOs in
// com.cs_42_3.surveyplatformbackend.survey.api.dto (FR-32~35).
// Keep these in sync with the backend when the real API is wired in.

export type QuestionType = 'SINGLE_CHOICE' | 'MULTI_CHOICE' | 'SCALE' | 'TEXT'

export interface QuestionOptionResponse {
  id: string
  optionText: string
  optionOrder: number
}

export interface QuestionResponse {
  id: string
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
  id: string
  type: QuestionType
  questionText: string
  updatedAt: string
}

export interface QuestionOptionRequest {
  optionText: string
}

export interface QuestionFormValues {
  type: QuestionType
  questionText: string
  required: boolean
  options: QuestionOptionRequest[]
  scaleMin: number | null
  scaleMax: number | null
  scaleMinLabel: string | null
  scaleMaxLabel: string | null
}

// Centralizes the display label per question type so pages don't each
// define their own copy and drift out of sync.
export const QUESTION_TYPE_LABELS: Record<QuestionType, string> = {
  SINGLE_CHOICE: 'Single choice',
  MULTI_CHOICE: 'Multiple choice',
  SCALE: 'Scale',
  TEXT: 'Text',
}
