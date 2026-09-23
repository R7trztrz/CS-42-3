import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import QuestionOptionsEditor from '../components/QuestionOptionsEditor'
import { createQuestion, getQuestion, updateQuestion } from '../api/questionBankApi'
import type { QuestionFormValues, QuestionType } from '../../../shared/types/question'

const EMPTY_FORM: QuestionFormValues = {
  type: 'SINGLE_CHOICE',
  questionText: '',
  required: true,
  options: [{ optionText: '' }, { optionText: '' }],
  scaleMin: 1,
  scaleMax: 10,
  scaleMinLabel: '',
  scaleMaxLabel: '',
}

export default function QuestionFormPage() {
  const { questionId } = useParams<{ questionId: string }>()
  const isEditing = Boolean(questionId)
  const navigate = useNavigate()

  const [form, setForm] = useState<QuestionFormValues>(EMPTY_FORM)
  const [isLoading, setIsLoading] = useState(isEditing)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!questionId) {
      return
    }
    getQuestion(questionId)
      .then((question) =>
        setForm({
          type: question.type,
          questionText: question.questionText,
          required: question.required,
          options: question.options.length
            ? question.options.map((o) => ({ optionText: o.optionText }))
            : [{ optionText: '' }, { optionText: '' }],
          scaleMin: question.scaleMin,
          scaleMax: question.scaleMax,
          scaleMinLabel: question.scaleMinLabel,
          scaleMaxLabel: question.scaleMaxLabel,
        }),
      )
      .catch((err) => setError(err instanceof Error ? err.message : '加载题目失败。'))
      .finally(() => setIsLoading(false))
  }, [questionId])

  function updateType(type: QuestionType) {
    setForm((prev) => ({
      ...prev,
      type,
      options: type === 'SCALE' || type === 'TEXT' ? prev.options : prev.options,
      scaleMin: type === 'SCALE' ? (prev.scaleMin ?? 1) : null,
      scaleMax: type === 'SCALE' ? (prev.scaleMax ?? 10) : null,
    }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      if (isEditing && questionId) {
        await updateQuestion(questionId, form)
      } else {
        await createQuestion(form)
      }
      navigate('/researcher/questions')
    } catch (err) {
      setError(err instanceof Error ? err.message : '保存题目失败。')
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) {
    return <p className="mx-auto max-w-2xl px-4 py-8 text-sm text-gray-500">加载中...</p>
  }

  const isChoiceType = form.type === 'SINGLE_CHOICE' || form.type === 'MULTI_CHOICE'

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-semibold text-gray-900">
        {isEditing ? '编辑题目' : '新建题目'}
      </h1>

      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">题目类型</label>
          <select
            value={form.type}
            onChange={(event) => updateType(event.target.value as QuestionType)}
            className="w-full rounded-lg border border-gray-300 px-3 py-2"
          >
            <option value="SINGLE_CHOICE">单选题</option>
            <option value="MULTI_CHOICE">多选题</option>
            <option value="SCALE">打分题</option>
            <option value="TEXT">文本题</option>
          </select>
          {isEditing && (
            <p className="mt-1 text-xs text-amber-600">
              如果这道题目正被某个问卷启用，后端会拒绝修改题型（见
              QuestionInUseException）。
            </p>
          )}
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">题目内容</label>
          <textarea
            value={form.questionText}
            onChange={(event) => setForm({ ...form, questionText: event.target.value })}
            rows={3}
            required
            className="w-full rounded-lg border border-gray-300 px-3 py-2 outline-none focus:border-blue-500"
          />
        </div>

        <label className="flex items-center gap-2 text-sm text-gray-700">
          <input
            type="checkbox"
            checked={form.required}
            onChange={(event) => setForm({ ...form, required: event.target.checked })}
          />
          必答
        </label>

        {isChoiceType && (
          <QuestionOptionsEditor
            options={form.options}
            onChange={(options) => setForm({ ...form, options })}
          />
        )}

        {form.type === 'SCALE' && (
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">最小值</label>
              <input
                type="number"
                value={form.scaleMin ?? ''}
                onChange={(event) =>
                  setForm({ ...form, scaleMin: Number(event.target.value) })
                }
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              />
              <input
                type="text"
                value={form.scaleMinLabel ?? ''}
                onChange={(event) => setForm({ ...form, scaleMinLabel: event.target.value })}
                placeholder="最小值标签（选填）"
                className="mt-2 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">最大值</label>
              <input
                type="number"
                value={form.scaleMax ?? ''}
                onChange={(event) =>
                  setForm({ ...form, scaleMax: Number(event.target.value) })
                }
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              />
              <input
                type="text"
                value={form.scaleMaxLabel ?? ''}
                onChange={(event) => setForm({ ...form, scaleMaxLabel: event.target.value })}
                placeholder="最大值标签（选填）"
                className="mt-2 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
            </div>
          </div>
        )}

        {error && (
          <div role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-lg bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isSubmitting ? '保存中...' : '保存题目'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/researcher/questions')}
            className="rounded-lg border border-gray-300 px-4 py-2 font-medium text-gray-700 hover:bg-gray-50"
          >
            取消
          </button>
        </div>
      </form>
    </div>
  )
}
