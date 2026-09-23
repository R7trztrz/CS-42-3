import { mockDelay } from '../../../shared/api/mockConfig'
import { mockQuestionBankSnapshot } from '../../questionBank/api/mockQuestionBank'
import type {
  QuestionnaireItemResponse,
  QuestionnaireResponse,
  SaveQuestionnaireRequest,
} from '../../../shared/types/questionnaire'

// mock 模式下全局只维护一份内存草稿（studyId 只是为了和真实接口签名保持一致
// 而接收，暂时没有实际按 study 区分）。
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

  // 第一遍：解析每个 item 对应的活的题库内容，并分配稳定 id，
  // 对应后端 Questionnaire#replaceItems 的两遍构建方式。
  const items: QuestionnaireItemResponse[] = request.items.map((itemRequest, position) => {
    if (seenQuestionIds.has(itemRequest.questionId)) {
      throw new Error(`题目 ${itemRequest.questionId} 被重复启用了。`)
    }
    seenQuestionIds.add(itemRequest.questionId)

    const question = bank.find((q) => q.id === itemRequest.questionId)
    if (!question) {
      throw new Error(`题库中找不到这道题目：${itemRequest.questionId}`)
    }

    return {
      id: `mock-item-${nextItemSeq++}`,
      position,
      question,
      branchRules: [],
    }
  })

  // 第二遍：所有 item 都有了稳定 id 之后再挂跳转规则，校验逻辑和后端
  // QuestionnaireServiceImpl 保持一致。
  request.items.forEach((itemRequest, position) => {
    const sourceItem = items[position]
    const question = sourceItem.question

    if (itemRequest.branchRules.length === 0) {
      return
    }
    if (question.type !== 'SINGLE_CHOICE' && question.type !== 'SCALE') {
      throw new Error(
        `只有单选题和打分题支持配置跳转规则：${question.id}`,
      )
    }

    const seenTriggers = new Set<string>()
    itemRequest.branchRules.forEach((rule) => {
      const hasOption = rule.sourceOptionId !== null
      const hasScale = rule.sourceScaleValue !== null
      if (hasOption === hasScale) {
        throw new Error('sourceOptionId 和 sourceScaleValue 必须恰好设置一个。')
      }

      const triggerKey = hasOption ? `opt:${rule.sourceOptionId}` : `scale:${rule.sourceScaleValue}`
      if (seenTriggers.has(triggerKey)) {
        throw new Error(`题目 ${question.id} 存在重复的跳转触发条件。`)
      }
      seenTriggers.add(triggerKey)

      if (hasOption && !question.options.some((o) => o.id === rule.sourceOptionId)) {
        throw new Error(`该选项不属于题目 ${question.id}。`)
      }
      if (
        hasScale &&
        (rule.sourceScaleValue! < (question.scaleMin ?? -Infinity) ||
          rule.sourceScaleValue! > (question.scaleMax ?? Infinity))
      ) {
        throw new Error(`刻度值超出题目 ${question.id} 配置的范围。`)
      }

      if (rule.targetPosition < 0 || rule.targetPosition >= items.length) {
        throw new Error(`跳转目标位置 ${rule.targetPosition} 越界。`)
      }
      if (rule.targetPosition === position) {
        throw new Error('跳转规则不能指向自己所在的题目。')
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
