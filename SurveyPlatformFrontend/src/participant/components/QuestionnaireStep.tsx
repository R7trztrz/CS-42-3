import { useEffect, useState } from 'react'

import { useParticipantQuestionnaire } from '../hooks/useParticipantQuestionnaire'
import type { ParticipantQuestion, SubmitAnswerPayload } from '../model/participantSession'

type QuestionnaireStepProps = {
  sessionToken: string
  onCompleteQuestionnaire: () => Promise<boolean>
  isCompleting: boolean
  completeErrorMessage: string | null
}

const EMPTY_ANSWER: SubmitAnswerPayload = {
  optionId: null,
  optionIds: null,
  scaleValue: null,
  textValue: null,
  unanswered: false,
}

// Local answer draft for the question currently on screen. This never
// decides what the *next* question is -- it only shapes the payload that
// gets PUT to the server, which returns the next question (or
// readyToSubmit) on its own.
function useAnswerDraft(question: ParticipantQuestion | null) {
  const [draft, setDraft] = useState<SubmitAnswerPayload>(EMPTY_ANSWER)

  // A new question (by itemId) always starts from a blank draft, even if
  // the participant is revisiting a position after a branch.
  useEffect(() => {
    setDraft(EMPTY_ANSWER)
  }, [question?.itemId])

  return [draft, setDraft] as const
}

function isAnswerPresent(question: ParticipantQuestion, draft: SubmitAnswerPayload): boolean {
  switch (question.questionType) {
    case 'SINGLE_CHOICE':
      return draft.optionId !== null
    case 'MULTI_CHOICE':
      return (draft.optionIds?.length ?? 0) > 0
    case 'SCALE':
      return draft.scaleValue !== null
    case 'TEXT':
      return Boolean(draft.textValue && draft.textValue.trim().length > 0)
    default:
      return false
  }
}

function QuestionnaireStep({
  sessionToken,
  onCompleteQuestionnaire,
  isCompleting,
  completeErrorMessage,
}: QuestionnaireStepProps) {
  const {
    currentQuestion,
    readyToSubmit,
    isLoading,
    isSubmitting,
    errorMessage,
    submitAnswer,
  } = useParticipantQuestionnaire(sessionToken)
  const [draft, setDraft] = useAnswerDraft(currentQuestion)

  if (isLoading && !currentQuestion) {
    return (
      <section className="mb-6 rounded-md border border-gray-200 bg-white px-6 py-6 shadow-sm">
        <p className="text-sm text-gray-600" role="status">
          Loading question...
        </p>
      </section>
    )
  }

  if (!currentQuestion) {
    return (
      <section className="mb-6 rounded-md border border-gray-200 bg-white px-6 py-6 shadow-sm">
        <p className="text-sm font-semibold text-emerald-700">
          You have answered every question.
        </p>
        {(errorMessage || completeErrorMessage) && (
          <p className="mt-2 text-sm text-red-700" role="alert">
            {errorMessage ?? completeErrorMessage}
          </p>
        )}
        <button
          type="button"
          disabled={!readyToSubmit || isCompleting}
          onClick={() => void onCompleteQuestionnaire()}
          className="mt-4 rounded-sm bg-emerald-700 px-5 py-2.5 text-sm font-semibold text-white hover:bg-emerald-800 disabled:opacity-60"
        >
          {isCompleting ? 'Submitting...' : 'Submit questionnaire'}
        </button>
      </section>
    )
  }

  const canSkip = !currentQuestion.required
  const canSubmit = isAnswerPresent(currentQuestion, draft)

  const submit = (answer: SubmitAnswerPayload) => {
    void submitAnswer(currentQuestion.itemId, answer)
  }

  return (
    <section className="mb-6 rounded-md border border-gray-200 bg-white px-6 py-6 shadow-sm">
      <p className="text-xs font-semibold uppercase text-emerald-700">
        Question {currentQuestion.position + 1}
      </p>
      <h2 className="mt-1 text-lg font-semibold text-[#172033]">{currentQuestion.text}</h2>
      {currentQuestion.required && (
        <p className="mt-1 text-xs font-medium text-gray-500">Required</p>
      )}

      <div className="mt-4">
        {currentQuestion.questionType === 'SINGLE_CHOICE' && (
          <div className="flex flex-col gap-2">
            {currentQuestion.options.map((option) => (
              <label key={option.optionId} className="flex items-center gap-2 text-sm text-gray-800">
                <input
                  type="radio"
                  name={`question-${currentQuestion.itemId}`}
                  checked={draft.optionId === option.optionId}
                  onChange={() => setDraft({ ...EMPTY_ANSWER, optionId: option.optionId })}
                />
                {option.text}
              </label>
            ))}
          </div>
        )}

        {currentQuestion.questionType === 'MULTI_CHOICE' && (
          <div className="flex flex-col gap-2">
            {currentQuestion.options.map((option) => {
              const selected = draft.optionIds?.includes(option.optionId) ?? false
              return (
                <label key={option.optionId} className="flex items-center gap-2 text-sm text-gray-800">
                  <input
                    type="checkbox"
                    checked={selected}
                    onChange={(event) => {
                      const current = draft.optionIds ?? []
                      const next = event.target.checked
                        ? [...current, option.optionId]
                        : current.filter((id) => id !== option.optionId)
                      setDraft({ ...EMPTY_ANSWER, optionIds: next })
                    }}
                  />
                  {option.text}
                </label>
              )
            })}
          </div>
        )}

        {currentQuestion.questionType === 'SCALE' && (
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between text-xs text-gray-500">
              <span>{currentQuestion.scaleMinLabel ?? currentQuestion.scaleMin}</span>
              <span>{currentQuestion.scaleMaxLabel ?? currentQuestion.scaleMax}</span>
            </div>
            <div className="flex flex-wrap gap-2">
              {Array.from(
                {
                  length: (currentQuestion.scaleMax ?? 0) - (currentQuestion.scaleMin ?? 0) + 1,
                },
                (_, index) => (currentQuestion.scaleMin ?? 0) + index,
              ).map((value) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => setDraft({ ...EMPTY_ANSWER, scaleValue: value })}
                  className={`h-10 w-10 rounded-sm border text-sm font-semibold ${
                    draft.scaleValue === value
                      ? 'border-emerald-700 bg-emerald-700 text-white'
                      : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  {value}
                </button>
              ))}
            </div>
          </div>
        )}

        {currentQuestion.questionType === 'TEXT' && (
          <textarea
            value={draft.textValue ?? ''}
            onChange={(event) => setDraft({ ...EMPTY_ANSWER, textValue: event.target.value })}
            rows={4}
            className="w-full rounded-sm border border-gray-300 px-3 py-2 text-sm"
          />
        )}
      </div>

      {errorMessage && (
        <p className="mt-3 text-sm font-semibold text-red-700" role="alert">
          {errorMessage}
        </p>
      )}

      <div className="mt-5 flex flex-wrap gap-3">
        <button
          type="button"
          disabled={!canSubmit || isSubmitting}
          onClick={() => submit(draft)}
          className="rounded-sm bg-emerald-700 px-5 py-2.5 text-sm font-semibold text-white hover:bg-emerald-800 disabled:opacity-60"
        >
          {isSubmitting ? 'Submitting...' : 'Next'}
        </button>
        {canSkip && (
          <button
            type="button"
            disabled={isSubmitting}
            onClick={() => submit({ ...EMPTY_ANSWER, unanswered: true })}
            className="rounded-sm border border-gray-300 bg-white px-5 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-60"
          >
            Skip
          </button>
        )}
      </div>
    </section>
  )
}

export default QuestionnaireStep
