// 对齐后端 com.cs_42_3.surveyplatformbackend.questionnaire.api.dto 里的
// DTO（FR-36~39）。设计说明见 SurveyPlatformBackend/docs/questionnaire-module.md。

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

// 编辑器本地使用的工作副本。请求/响应里跳转规则的目标是用数组下标表示的，
// 研究者一旦重新排序题目下标就会失效；编辑器改为给每一行保留稳定的
// clientId，只在真正调用 saveQuestionnaire 前才解析成目标下标。
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
