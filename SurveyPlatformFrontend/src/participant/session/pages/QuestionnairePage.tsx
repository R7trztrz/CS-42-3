import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSession } from '../state/SessionContext'
import type { QuestionnaireItemResponse } from '../../../shared/types/questionnaire'
import type { AnswerSubmission } from '../types'

// UC-31：展示当前题目，提交后把答案交给 SessionContext —— 它会通过
// resolveNextItem（FR-38 跳转表查找）解析出下一题并前进；如果没有下一题
// 了，就把会话标记为完成。
export default function QuestionnairePage() {
  const { questionnaire, currentItem, isComplete, submitCurrentAnswer } = useSession()
  const navigate = useNavigate()

  useEffect(() => {
    if (isComplete) {
      navigate('../complete', { replace: true })
    }
  }, [isComplete, navigate])

  if (!questionnaire || questionnaire.items.length === 0) {
    return (
      <div className="mx-auto max-w-xl px-4 py-16 text-center text-sm text-gray-500">
        这个研究还没有配置问卷。
      </div>
    )
  }

  if (!currentItem) {
    return null
  }

  const answeredCount = questionnaire.items.findIndex((i) => i.id === currentItem.id)

  return (
    <div className="mx-auto max-w-xl px-4 py-8">
      <p className="mb-2 text-xs font-medium text-gray-400">
        第 {answeredCount + 1} 题，共 {questionnaire.items.length} 题
      </p>

      {/* 以题目 id 作为 key，切换题目（包括跳转产生的切换）时会重新挂载，
          每个答案输入组件都是全新状态，不需要额外用 effect 去重置。 */}
      <QuestionAnswerForm
        key={currentItem.id}
        item={currentItem}
        onSubmit={submitCurrentAnswer}
      />
    </div>
  )
}

interface QuestionAnswerFormProps {
  item: QuestionnaireItemResponse
  onSubmit: (answer: Omit<AnswerSubmission, 'itemId'>) => Promise<void>
}

function QuestionAnswerForm({ item, onSubmit }: QuestionAnswerFormProps) {
  const { question } = item

  const [selectedOptionId, setSelectedOptionId] = useState<string | null>(null)
  const [selectedOptionIds, setSelectedOptionIds] = useState<string[]>([])
  const [scaleValue, setScaleValue] = useState<number | null>(question.scaleMin ?? null)
  const [textAnswer, setTextAnswer] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit() {
    setIsSubmitting(true)
    try {
      await onSubmit(
        question.type === 'SINGLE_CHOICE'
          ? { selectedOptionId: selectedOptionId ?? undefined }
          : question.type === 'MULTI_CHOICE'
            ? { selectedOptionIds }
            : question.type === 'SCALE'
              ? { scaleValue: scaleValue ?? undefined }
              : { textAnswer },
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  const canSubmit =
    !question.required ||
    (question.type === 'SINGLE_CHOICE' && selectedOptionId !== null) ||
    (question.type === 'MULTI_CHOICE' && selectedOptionIds.length > 0) ||
    (question.type === 'SCALE' && scaleValue !== null) ||
    (question.type === 'TEXT' && textAnswer.trim().length > 0)

  return (
    <div>
      <h1 className="mb-6 text-xl font-semibold text-gray-900">
        {question.questionText}
        {question.required && <span className="ml-1 text-red-500">*</span>}
      </h1>

      {question.type === 'SINGLE_CHOICE' && (
        <div className="space-y-2">
          {question.options.map((option) => (
            <label
              key={option.id}
              className="flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-3 text-sm hover:border-blue-400"
            >
              <input
                type="radio"
                name="single-choice"
                checked={selectedOptionId === option.id}
                onChange={() => setSelectedOptionId(option.id)}
              />
              {option.optionText}
            </label>
          ))}
        </div>
      )}

      {question.type === 'MULTI_CHOICE' && (
        <div className="space-y-2">
          {question.options.map((option) => (
            <label
              key={option.id}
              className="flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-3 text-sm hover:border-blue-400"
            >
              <input
                type="checkbox"
                checked={selectedOptionIds.includes(option.id)}
                onChange={(event) =>
                  setSelectedOptionIds((prev) =>
                    event.target.checked
                      ? [...prev, option.id]
                      : prev.filter((id) => id !== option.id),
                  )
                }
              />
              {option.optionText}
            </label>
          ))}
        </div>
      )}

      {question.type === 'SCALE' && (
        <div>
          <input
            type="range"
            min={question.scaleMin ?? 0}
            max={question.scaleMax ?? 10}
            value={scaleValue ?? question.scaleMin ?? 0}
            onChange={(event) => setScaleValue(Number(event.target.value))}
            className="w-full"
          />
          <div className="mt-1 flex justify-between text-xs text-gray-500">
            <span>{question.scaleMinLabel ?? question.scaleMin}</span>
            <span className="font-medium text-gray-800">{scaleValue}</span>
            <span>{question.scaleMaxLabel ?? question.scaleMax}</span>
          </div>
        </div>
      )}

      {question.type === 'TEXT' && (
        <textarea
          value={textAnswer}
          onChange={(event) => setTextAnswer(event.target.value)}
          rows={4}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 outline-none focus:border-blue-500"
        />
      )}

      <button
        type="button"
        onClick={handleSubmit}
        disabled={!canSubmit || isSubmitting}
        className="mt-6 w-full rounded-lg bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {isSubmitting ? '提交中...' : '下一题'}
      </button>
    </div>
  )
}
