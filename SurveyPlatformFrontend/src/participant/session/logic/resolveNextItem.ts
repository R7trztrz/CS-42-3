// 对齐 SurveyPlatformBackend/docs/questionnaire-module.md 第 5 节里给 M5
// 描述的运行时算法（客户端版本）：
//
//   1. 按（当前题目，选中的答案）去查 questionnaire_branch_rules
//   2. 命中一条规则 -> 跳到该规则的目标题目
//   3. 没命中 -> 走默认顺序，即下一道展示顺序上的题目
//   4. 没有下一题了 -> 问卷结束
//
// 这段逻辑存在的意义是让参与者端 UI 能在真正的 M5 后端接口上线前，
// 把分支跳转完整地预览一遍。等真实接口落地后，这个解析过程应该移到
// 服务端（后端才是"哪条规则生效"的权威来源，而且它可以基于发布时冻结的
// 快照而不是这里读到的活的草稿）——到那时这个函数最多只应该作为
// 客户端预览/乐观更新的辅助手段保留，甚至可以直接删掉。
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
