// Client-side mirror of the runtime algorithm described in
// SurveyPlatformBackend/docs/m4-api-contract-v1.md section 7.3:
//
//   1. Look up the current item's branchRules by the submitted answer
//   2. Match -> jump to that rule's targetItemId
//   3. No match -> follow the item's own defaultNextItemId (the backend
//      already resolved "next item in display order" for us)
//   4. defaultNextItemId is null -> the questionnaire is done
//
// This exists so the participant UI can preview branching end-to-end while
// the real M5 backend endpoint doesn't exist yet (M4 v1 has no public
// participant endpoint - see the contract's section 6). Once one lands,
// this resolution should move server-side; this function should then only
// be used as a client-side preview/optimistic-UI helper, if kept at all.
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

  const nextItemId = matchedRule ? matchedRule.targetItemId : currentItem.defaultNextItemId
  if (!nextItemId) {
    return null
  }
  return items.find((item) => item.itemId === nextItemId) ?? null
}
