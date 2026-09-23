import { mockDelay } from '../../../shared/api/mockConfig'
import { mockQuestionBankSnapshot } from '../../questionBank/api/mockQuestionBank'
import type {
  QuestionnaireItemResponse,
  QuestionnaireResponse,
  SaveQuestionnaireRequest,
} from '../../../shared/types/questionnaire'

// One in-memory draft per mock session (studyId is accepted for API-shape
// parity but ignored, since study scoping isn't wired up in mock mode).
let draft: QuestionnaireItemResponse[] = []
let nextItemSeq = 1
let nextRuleSeq = 1

export async function mockGetQuestionnaire(studyId: string): Promise<QuestionnaireResponse> {
  await mockDelay()
  return {
    id: draft.length ? 'mock-questionnaire' : null,
    studyId,
    items: draft,
    createdAt: draft.length ? new Date().toISOString() : null,
    updatedAt: draft.length ? new Date().toISOString() : null,
  }
}

export async function mockSaveQuestionnaire(
  studyId: string,
  request: SaveQuestionnaireRequest,
): Promise<QuestionnaireResponse> {
  await mockDelay()

  const bank = mockQuestionBankSnapshot()
  const seenQuestionIds = new Set<string>()

  // Pass 1: resolve each item's live bank content and assign a stable id,
  // mirroring the backend's two-pass build in Questionnaire#replaceItems.
  const items: QuestionnaireItemResponse[] = request.items.map((itemRequest, position) => {
    if (seenQuestionIds.has(itemRequest.questionId)) {
      throw new Error(`Question ${itemRequest.questionId} is enabled more than once.`)
    }
    seenQuestionIds.add(itemRequest.questionId)

    const question = bank.find((q) => q.id === itemRequest.questionId)
    if (!question) {
      throw new Error(`Question not found in your question bank: ${itemRequest.questionId}`)
    }

    return {
      id: `mock-item-${nextItemSeq++}`,
      position,
      question,
      branchRules: [],
    }
  })

  // Pass 2: attach branch rules now that every item has a resolved id,
  // applying the same eligibility/trigger/target rules as
  // QuestionnaireServiceImpl on the backend.
  request.items.forEach((itemRequest, position) => {
    const sourceItem = items[position]
    const question = sourceItem.question

    if (itemRequest.branchRules.length === 0) {
      return
    }
    if (question.type !== 'SINGLE_CHOICE' && question.type !== 'SCALE') {
      throw new Error(
        `Branch rules are only supported for SINGLE_CHOICE and SCALE questions: ${question.id}`,
      )
    }

    const seenTriggers = new Set<string>()
    itemRequest.branchRules.forEach((rule) => {
      const hasOption = rule.sourceOptionId !== null
      const hasScale = rule.sourceScaleValue !== null
      if (hasOption === hasScale) {
        throw new Error('Exactly one of sourceOptionId or sourceScaleValue is required.')
      }

      const triggerKey = hasOption ? `opt:${rule.sourceOptionId}` : `scale:${rule.sourceScaleValue}`
      if (seenTriggers.has(triggerKey)) {
        throw new Error(`Duplicate branch rule trigger on question ${question.id}.`)
      }
      seenTriggers.add(triggerKey)

      if (hasOption && !question.options.some((o) => o.id === rule.sourceOptionId)) {
        throw new Error(`Option does not belong to question ${question.id}.`)
      }
      if (
        hasScale &&
        (rule.sourceScaleValue! < (question.scaleMin ?? -Infinity) ||
          rule.sourceScaleValue! > (question.scaleMax ?? Infinity))
      ) {
        throw new Error(`Scale value is outside the configured range for question ${question.id}.`)
      }

      if (rule.targetPosition < 0 || rule.targetPosition >= items.length) {
        throw new Error(`Branch target position ${rule.targetPosition} is out of range.`)
      }
      if (rule.targetPosition === position) {
        throw new Error('A branch rule cannot target its own item.')
      }

      sourceItem.branchRules.push({
        id: `mock-rule-${nextRuleSeq++}`,
        sourceOptionId: rule.sourceOptionId,
        sourceScaleValue: rule.sourceScaleValue,
        targetItemId: items[rule.targetPosition].id,
        targetPosition: rule.targetPosition,
      })
    })
  })

  draft = items
  return {
    id: 'mock-questionnaire',
    studyId,
    items: draft,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }
}
