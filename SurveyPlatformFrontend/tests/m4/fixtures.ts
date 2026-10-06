import type {
  QuestionResponse,
  QuestionSummaryResponse,
  QuestionType,
} from '../../src/shared/types/question'
import type {
  QuestionnaireBranchRuleResponse,
  QuestionnaireItemResponse,
  QuestionnaireResponse,
} from '../../src/shared/types/questionnaire'

export function makeQuestion(
  questionId: string,
  questionText: string,
  type: QuestionType = 'TEXT',
): QuestionResponse {
  return {
    questionId,
    type,
    questionText,
    required: true,
    options:
      type === 'SINGLE_CHOICE' || type === 'MULTI_CHOICE'
        ? [
            { optionId: `${questionId}-option-1`, optionText: 'Yes', optionOrder: 0 },
            { optionId: `${questionId}-option-2`, optionText: 'No', optionOrder: 1 },
          ]
        : [],
    scaleMin: type === 'SCALE' ? 1 : null,
    scaleMax: type === 'SCALE' ? 5 : null,
    scaleMinLabel: type === 'SCALE' ? 'Low' : null,
    scaleMaxLabel: type === 'SCALE' ? 'High' : null,
    createdAt: '2026-10-05T00:00:00Z',
    updatedAt: '2026-10-05T01:00:00Z',
  }
}

export function makeQuestionSummary(
  questionId: string,
  questionText: string,
  type: QuestionType = 'TEXT',
): QuestionSummaryResponse {
  return {
    questionId,
    questionText,
    type,
    updatedAt: '2026-10-05T01:00:00Z',
  }
}

export function makeQuestionnaireItem(
  itemId: string,
  position: number,
  question: QuestionResponse | null,
  branchRules: QuestionnaireBranchRuleResponse[] = [],
): QuestionnaireItemResponse {
  return {
    itemId,
    position,
    missing: question === null,
    question,
    referenceStatus: question === null ? 'MISSING_QUESTION' : 'VALID',
    branchRules,
    defaultNextItemId: null,
  }
}

export function makeQuestionnaire(
  items: QuestionnaireItemResponse[],
  overrides: Partial<QuestionnaireResponse> = {},
): QuestionnaireResponse {
  return {
    questionnaireId: 'questionnaire-1',
    snapshotId: null,
    studyId: 'study-1',
    contentSource: 'LIVE_DRAFT',
    items,
    version: 1,
    updatedAt: '2026-10-05T01:00:00Z',
    publishedAt: null,
    valid: true,
    validationIssues: [],
    ...overrides,
  }
}
