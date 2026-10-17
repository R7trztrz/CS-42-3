import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import ResearcherHeader from '../../../components/researcher/ResearcherHeader'
import QuestionOptionsEditor from '../components/QuestionOptionsEditor'
import { createQuestion, getQuestion, updateQuestion } from '../api/questionBankApi'
import { describeError } from '../../../shared/types/apiError'
import type { QuestionFormValues, QuestionType } from '../../../shared/types/question'
import { validateQuestionForm } from '../model/questionFormValidation'
import './QuestionFormPage.css'

function QuestionFormWorkspace({ children }: { children: ReactNode }) {
  return (
    <div className="qf-workspace min-h-screen bg-[#f3f6f8] text-[#172033]">
      <ResearcherHeader activePage="questions" />
      <main className="mx-auto w-full max-w-7xl px-5 py-8 md:px-8 md:py-10">
        <nav aria-label="Question breadcrumb" className="qf-breadcrumb">
          <Link to="/questions"><span aria-hidden="true">&larr;</span> Back to question bank</Link>
          <span aria-hidden="true">/</span><span>Question editor</span>
        </nav>
        {children}
      </main>
    </div>
  )
}

const EMPTY_FORM: QuestionFormValues = {
  type: 'SINGLE_CHOICE',
  questionText: '',
  required: true,
  options: [
    { optionId: null, optionText: '' },
    { optionId: null, optionText: '' },
  ],
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
            ? question.options.map((o) => ({ optionId: o.optionId, optionText: o.optionText }))
            : [
                { optionId: null, optionText: '' },
                { optionId: null, optionText: '' },
              ],
          scaleMin: question.scaleMin,
          scaleMax: question.scaleMax,
          scaleMinLabel: question.scaleMinLabel,
          scaleMaxLabel: question.scaleMaxLabel,
        }),
      )
      .catch((err) => setError(describeError(err, 'Failed to load question.')))
      .finally(() => setIsLoading(false))
  }, [questionId])

  function updateType(type: QuestionType) {
    setForm((prev) => ({
      ...prev,
      type,
      scaleMin: type === 'SCALE' ? (prev.scaleMin ?? 1) : null,
      scaleMax: type === 'SCALE' ? (prev.scaleMax ?? 10) : null,
    }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    const validationErrors = validateQuestionForm(form)
    if (validationErrors.length > 0) {
      setError(validationErrors.join(' '))
      return
    }
    setIsSubmitting(true)
    try {
      if (isEditing && questionId) {
        await updateQuestion(questionId, form)
      } else {
        await createQuestion(form)
      }
      navigate('/questions')
    } catch (err) {
      setError(describeError(err, 'Failed to save question.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) {
    return <QuestionFormWorkspace><p role="status" className="text-sm text-gray-500">Loading question...</p></QuestionFormWorkspace>
  }

  const isChoiceType = form.type === 'SINGLE_CHOICE' || form.type === 'MULTI_CHOICE'

  return (
    <QuestionFormWorkspace>
      <div className="qf-heading">
      <p className="mb-2 text-sm font-semibold text-emerald-700">Question bank</p>
      <h2 className="text-3xl font-semibold">
        {isEditing ? 'Edit question' : 'New question'}
      </h2>
      </div>

      <form onSubmit={handleSubmit} className="qf-form space-y-6">
        <fieldset disabled={isSubmitting} className="space-y-6 min-w-0">
        <div>
          <label htmlFor="question-type" className="mb-2 block text-sm font-semibold text-gray-700">Question type</label>
          <select
            id="question-type"
            value={form.type}
            onChange={(event) => updateType(event.target.value as QuestionType)}
            className="w-full rounded-lg border border-gray-300 px-3 py-2"
          >
            <option value="SINGLE_CHOICE">Single choice</option>
            <option value="MULTI_CHOICE">Multiple choice</option>
            <option value="SCALE">Scale</option>
            <option value="TEXT">Text</option>
          </select>
          {isEditing && (
            <p className="mt-1 text-xs text-amber-600">
              A questionnaire draft that already enables this question reads it live, so
              changing its type or options here takes effect immediately for any draft, but
              never for a questionnaire that has already been published.
            </p>
          )}
        </div>

        <div>
          <label htmlFor="question-text" className="mb-2 block text-sm font-semibold text-gray-700">Question text</label>
          <textarea
            id="question-text"
            value={form.questionText}
            onChange={(event) => setForm({ ...form, questionText: event.target.value })}
            rows={3}
            required
            className="w-full border border-gray-300 px-3 py-2"
          />
        </div>

        <label className="qf-required flex items-center gap-3 text-sm text-gray-700">
          <input
            type="checkbox"
            checked={form.required}
            onChange={(event) => setForm({ ...form, required: event.target.checked })}
          />
          Required
        </label>

        {isChoiceType && (
          <QuestionOptionsEditor
            options={form.options}
            onChange={(options) => setForm({ ...form, options })}
          />
        )}

        {form.type === 'SCALE' && (
          <div className="qf-scale grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="scale-min" className="mb-2 block text-sm font-semibold text-gray-700">Minimum</label>
              <input
                id="scale-min"
                type="number"
                value={form.scaleMin ?? ''}
                onChange={(event) =>
                  setForm({
                    ...form,
                    scaleMin: event.target.value === '' ? null : Number(event.target.value),
                  })
                }
                step={1}
                required
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              />
              <input
                type="text"
                aria-label="Minimum label"
                value={form.scaleMinLabel ?? ''}
                onChange={(event) => setForm({ ...form, scaleMinLabel: event.target.value })}
                placeholder="Minimum label (optional)"
                className="mt-2 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label htmlFor="scale-max" className="mb-2 block text-sm font-semibold text-gray-700">Maximum</label>
              <input
                id="scale-max"
                type="number"
                value={form.scaleMax ?? ''}
                onChange={(event) =>
                  setForm({
                    ...form,
                    scaleMax: event.target.value === '' ? null : Number(event.target.value),
                  })
                }
                step={1}
                required
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              />
              <input
                type="text"
                aria-label="Maximum label"
                value={form.scaleMaxLabel ?? ''}
                onChange={(event) => setForm({ ...form, scaleMaxLabel: event.target.value })}
                placeholder="Maximum label (optional)"
                className="mt-2 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
            </div>
          </div>
        )}
        </fieldset>

        {error && (
          <div role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        <div className="qf-actions flex flex-wrap gap-3">
          <button
            type="submit"
            disabled={isSubmitting}
            className="qf-button qf-button--primary"
          >
            {isSubmitting ? 'Saving...' : 'Save question'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/questions')}
            className="qf-button qf-button--secondary"
          >
            Cancel
          </button>
        </div>
      </form>
    </QuestionFormWorkspace>
  )
}
