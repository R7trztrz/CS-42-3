// 对齐后端 com.cs_42_3.surveyplatformbackend.survey.api.dto 里的 DTO（FR-32~35）。
// 接入真实接口时要注意和后端保持同步。

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

// 集中维护题型的中文展示名，避免各页面各自定义导致不一致。
export const QUESTION_TYPE_LABELS: Record<QuestionType, string> = {
  SINGLE_CHOICE: '单选题',
  MULTI_CHOICE: '多选题',
  SCALE: '打分题',
  TEXT: '文本题',
}
