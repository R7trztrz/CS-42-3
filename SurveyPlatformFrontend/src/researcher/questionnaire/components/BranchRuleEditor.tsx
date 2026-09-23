import { QUESTION_TYPE_LABELS, type QuestionResponse } from '../../../shared/types/question'
import type {
  QuestionnaireEditorBranchRule,
  QuestionnaireEditorItem,
} from '../../../shared/types/questionnaire'

interface BranchRuleEditorProps {
  question: QuestionResponse
  branchRules: QuestionnaireEditorBranchRule[]
  otherItems: QuestionnaireEditorItem[]
  onChange: (branchRules: QuestionnaireEditorBranchRule[]) => void
}

// Only SINGLE_CHOICE/SCALE questions can carry branch rules (see FR-38
// design doc: MULTI_CHOICE would let one answer select several options with
// conflicting targets, which breaks NFR-15 determinism).
export default function BranchRuleEditor({
  question,
  branchRules,
  otherItems,
  onChange,
}: BranchRuleEditorProps) {
  if (question.type !== 'SINGLE_CHOICE' && question.type !== 'SCALE') {
    return (
      <p className="text-xs text-gray-400">
        Branch rules aren't available for {QUESTION_TYPE_LABELS[question.type].toLowerCase()}{' '}
        questions.
      </p>
    )
  }

  if (otherItems.length === 0) {
    return <p className="text-xs text-gray-400">Add another item to configure a jump target.</p>
  }

  const triggerOptions =
    question.type === 'SINGLE_CHOICE'
      ? question.options.map((o) => ({ value: o.id, label: o.optionText }))
      : Array.from(
          { length: (question.scaleMax ?? 0) - (question.scaleMin ?? 0) + 1 },
          (_, i) => (question.scaleMin ?? 0) + i,
        ).map((value) => ({ value: String(value), label: String(value) }))

  const usedTriggers = new Set(branchRules.map((r) => r.sourceOptionId ?? String(r.sourceScaleValue)))
  const availableTriggers = triggerOptions.filter((t) => !usedTriggers.has(String(t.value)))

  function addRule() {
    const first = availableTriggers[0]
    if (!first) return
    const rule: QuestionnaireEditorBranchRule =
      question.type === 'SINGLE_CHOICE'
        ? { sourceOptionId: String(first.value), sourceScaleValue: null, targetClientId: otherItems[0].clientId }
        : { sourceOptionId: null, sourceScaleValue: Number(first.value), targetClientId: otherItems[0].clientId }
    onChange([...branchRules, rule])
  }

  function updateRule(index: number, patch: Partial<QuestionnaireEditorBranchRule>) {
    const next = branchRules.slice()
    next[index] = { ...next[index], ...patch }
    onChange(next)
  }

  function removeRule(index: number) {
    onChange(branchRules.filter((_, i) => i !== index))
  }

  return (
    <div className="space-y-2 rounded-lg bg-gray-50 p-3">
      <p className="text-xs font-medium text-gray-600">
        Jump rules (default: continue to the next item)
      </p>

      {branchRules.map((rule, index) => (
        <div key={index} className="flex flex-wrap items-center gap-2 text-sm">
          <span className="text-gray-500">If answer is</span>
          <select
            value={rule.sourceOptionId ?? String(rule.sourceScaleValue)}
            onChange={(event) =>
              updateRule(
                index,
                question.type === 'SINGLE_CHOICE'
                  ? { sourceOptionId: event.target.value, sourceScaleValue: null }
                  : { sourceOptionId: null, sourceScaleValue: Number(event.target.value) },
              )
            }
            className="rounded border border-gray-300 px-2 py-1"
          >
            {triggerOptions.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
          <span className="text-gray-500">jump to</span>
          <select
            value={rule.targetClientId}
            onChange={(event) => updateRule(index, { targetClientId: event.target.value })}
            className="rounded border border-gray-300 px-2 py-1"
          >
            {otherItems.map((item, i) => (
              <option key={item.clientId} value={item.clientId}>
                Q{i + 1}: {item.question.questionText.slice(0, 30)}
              </option>
            ))}
          </select>
          <button
            type="button"
            onClick={() => removeRule(index)}
            className="text-red-600 hover:underline"
          >
            Remove
          </button>
        </div>
      ))}

      <button
        type="button"
        onClick={addRule}
        disabled={availableTriggers.length === 0}
        className="rounded border border-dashed border-gray-300 px-2 py-1 text-xs text-gray-600 hover:border-blue-400 hover:text-blue-600 disabled:cursor-not-allowed disabled:opacity-40"
      >
        + Add jump rule
      </button>
    </div>
  )
}
