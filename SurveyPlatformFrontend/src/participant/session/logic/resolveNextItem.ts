// Client-side mirror of the runtime algorithm described for M5 in
// SurveyPlatformBackend/docs/questionnaire-module.md section 5:
//
//   1. Look up questionnaire_branch_rules by (current item, selected answer)
//   2. Match -> jump to that rule's target item
//   3. No match -> continue to the next item in display order
//   4. No next item -> the questionnaire is done
//
// This exists so the participant UI can preview branching end-to-end while
// the real M5 backend endpoint doesn't exist yet. Once that endpoint lands,
// this resolution should move server-side (the backend is the source of
// truth for which rules apply, and it can act on a frozen publish snapshot
// instead of the live draft this mock reads from) - this function should
// then only be used as a client-side preview/optimistic-UI helper, if kept
// at all.
import type { QuestionnaireItemResponse } from '../../../shared/types/questionnaire'
import type { AnswerSubmission } from '../types'

export function resolveNextItem(
  items: QuestionnaireItemResponse[],
  currentItem: QuestionnaireItemResponse,
  answer: Omit<AnswerSubmission, 'itemId'>,
): QuestionnaireItemResponse | null {
  const matchedRule = currentItem.branchRules.find((rule) => {
    if (rule.sourceOptionId !== null) {
      return rule.sourceOptionId === answer.selectedOptionId
    }
    if (rule.sourceScaleValue !== null) {
      return rule.sourceScaleValue === answer.scaleValue
    }
    return false
  })

  if (matchedRule) {
    return items.find((item) => item.id === matchedRule.targetItemId) ?? null
  }

  return items.find((item) => item.position === currentItem.position + 1) ?? null
}
