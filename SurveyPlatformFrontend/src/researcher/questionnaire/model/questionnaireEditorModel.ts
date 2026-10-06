import type { ApiErrorDetail } from '../../../shared/types/apiError'
import type {
  QuestionnaireEditorBranchRule,
  QuestionnaireEditorItem,
  QuestionnaireItemResponse,
  QuestionnaireValidationIssue,
  SaveQuestionnaireRequest,
} from '../../../shared/types/questionnaire'

export const QUESTIONNAIRE_HISTORY_LIMIT = 20

export interface QuestionnaireEditorHistory {
  items: QuestionnaireEditorItem[]
  past: QuestionnaireEditorItem[][]
}

export type QuestionnaireEditorAction =
  | { type: 'reset'; items: QuestionnaireEditorItem[] }
  | { type: 'append'; item: QuestionnaireEditorItem }
  | { type: 'remove'; clientId: string }
  | { type: 'move'; index: number; direction: -1 | 1 }
  | {
      type: 'replace-branch-rules'
      clientId: string
      branchRules: QuestionnaireEditorBranchRule[]
    }
  | { type: 'undo' }

export interface QuestionnaireIssueView {
  code: string
  message: string
  itemIndex: number | null
  itemId: string | null
  ruleIndex: number | null
  field: string | null
}

export interface BranchTargetOption {
  item: QuestionnaireEditorItem
  position: number
}

function commitItems(
  state: QuestionnaireEditorHistory,
  items: QuestionnaireEditorItem[],
): QuestionnaireEditorHistory {
  if (items === state.items || questionnaireItemsFingerprint(items) === questionnaireItemsFingerprint(state.items)) {
    return state
  }

  return {
    items,
    past: [...state.past, state.items].slice(-QUESTIONNAIRE_HISTORY_LIMIT),
  }
}

export function questionnaireEditorReducer(
  state: QuestionnaireEditorHistory,
  action: QuestionnaireEditorAction,
): QuestionnaireEditorHistory {
  switch (action.type) {
    case 'reset':
      return { items: action.items, past: [] }
    case 'append':
      return commitItems(state, [...state.items, action.item])
    case 'remove':
      return commitItems(
        state,
        state.items
          .filter((item) => item.clientId !== action.clientId)
          .map((item) => ({
            ...item,
            branchRules: item.branchRules.filter(
              (rule) => rule.targetClientId !== action.clientId,
            ),
          })),
      )
    case 'move': {
      const target = action.index + action.direction
      if (target < 0 || target >= state.items.length) {
        return state
      }
      const items = state.items.slice()
      ;[items[action.index], items[target]] = [items[target], items[action.index]]
      return commitItems(state, items)
    }
    case 'replace-branch-rules':
      return commitItems(
        state,
        state.items.map((item) =>
          item.clientId === action.clientId
            ? { ...item, branchRules: action.branchRules }
            : item,
        ),
      )
    case 'undo': {
      const previous = state.past.at(-1)
      if (!previous) {
        return state
      }
      return { items: previous, past: state.past.slice(0, -1) }
    }
  }
}

export function questionnaireItemsFingerprint(items: QuestionnaireEditorItem[]): string {
  return JSON.stringify(
    items.map((item) => ({
      itemId: item.itemId,
      questionId: item.questionId,
      branchRules: item.branchRules,
    })),
  )
}

export function buildSaveQuestionnaireRequest(
  items: QuestionnaireEditorItem[],
  expectedVersion: number | null,
): SaveQuestionnaireRequest {
  const positionByClientId = new Map(
    items.map((item, index) => [item.clientId, index]),
  )

  return {
    expectedVersion,
    items: items.map((item) => ({
      itemId: item.itemId,
      questionId: item.questionId,
      branchRules: item.branchRules.map((rule) => ({
        sourceOptionId: rule.sourceOptionId,
        sourceScaleValue: rule.sourceScaleValue,
        targetPosition: positionByClientId.get(rule.targetClientId) ?? 0,
      })),
    })),
  }
}

/**
 * Rebuilds the local editor rows from a GET/PUT response. Existing client IDs
 * survive saves while server-assigned item IDs are copied back immediately,
 * so the next PUT retains those identities without requiring a reload.
 */
export function hydrateQuestionnaireItems(
  responseItems: QuestionnaireItemResponse[],
  previousItems: QuestionnaireEditorItem[],
  createClientId: () => string,
): QuestionnaireEditorItem[] {
  const previousByItemId = new Map(
    previousItems
      .filter((item): item is QuestionnaireEditorItem & { itemId: string } => item.itemId !== null)
      .map((item) => [item.itemId, item]),
  )
  const usedClientIds = new Set<string>()

  const clientIds = responseItems.map((responseItem, index) => {
    const identityMatch = previousByItemId.get(responseItem.itemId)
    const positionalMatch = previousItems[index]
    const previous =
      identityMatch ??
      (positionalMatch?.questionId === (responseItem.question?.questionId ?? '')
        ? positionalMatch
        : previousItems.find(
            (item) =>
              !usedClientIds.has(item.clientId) &&
              item.questionId === (responseItem.question?.questionId ?? ''),
          ))
    const clientId = previous?.clientId ?? createClientId()
    usedClientIds.add(clientId)
    return clientId
  })

  const clientIdByItemId = new Map(
    responseItems.map((item, index) => [item.itemId, clientIds[index]]),
  )

  return responseItems.map((item, index) => ({
    clientId: clientIds[index],
    itemId: item.itemId,
    questionId: item.question?.questionId ?? previousItems[index]?.questionId ?? '',
    question: item.question,
    missing: item.missing,
    branchRules: item.branchRules.map((rule) => ({
      sourceOptionId: rule.sourceOptionId,
      sourceScaleValue: rule.sourceScaleValue,
      targetClientId: clientIdByItemId.get(rule.targetItemId) ?? clientIds[index],
    })),
  }))
}

export function getBranchTargetOptions(
  items: QuestionnaireEditorItem[],
  sourceClientId: string,
): BranchTargetOption[] {
  return items.flatMap((item, position) =>
    item.clientId !== sourceClientId && item.question !== null
      ? [{ item, position }]
      : [],
  )
}

export function validationIssueToView(
  issue: QuestionnaireValidationIssue,
): QuestionnaireIssueView {
  return {
    code: issue.code,
    message: issue.message,
    itemIndex: issue.itemIndex,
    itemId: issue.itemId,
    ruleIndex: issue.ruleIndex,
    field: null,
  }
}

export function apiErrorDetailToView(detail: ApiErrorDetail): QuestionnaireIssueView {
  return {
    code: detail.code,
    message: detail.message,
    itemIndex: detail.index,
    itemId: detail.itemId,
    ruleIndex: detail.ruleIndex,
    field: detail.field || null,
  }
}

export function questionnaireIssueLocation(issue: QuestionnaireIssueView): string[] {
  const parts: string[] = []
  if (issue.itemIndex !== null) parts.push(`Q${issue.itemIndex + 1}`)
  if (issue.itemId !== null) parts.push(`itemId ${issue.itemId}`)
  if (issue.ruleIndex !== null) parts.push(`rule ${issue.ruleIndex + 1}`)
  if (issue.field !== null) parts.push(`field ${issue.field}`)
  parts.push(`code ${issue.code}`)
  return parts
}
