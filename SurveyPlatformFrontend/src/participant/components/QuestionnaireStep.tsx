import { useState } from 'react'

import { QuestionnaireView } from '../../pages/participant/ParticipantDemoTemplate'
import FinishedStep from './FinishedStep'
import { useParticipantQuestionnaire, type UseParticipantQuestionnaireResult } from '../hooks/useParticipantQuestionnaire'
import type { ParticipantQuestion, SubmitAnswerPayload } from '../model/participantSession'

type QuestionnaireStepProps = {
  studyTitle?: string
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
  studyTitle,
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
    isClosed,
    submitAnswer,
    reload,
  } = useParticipantQuestionnaire(sessionToken)
  const description = studyTitle
    ? `Please answer the questions for "${studyTitle}" about your experience.`
    : 'Please answer the following questions about your experience.'

  if (isClosed) return <FinishedStep status="ABANDONED" abandonmentReason="STUDY_CLOSED" />

  if (!currentQuestion && !readyToSubmit) {
    return (
      <QuestionnaireView description={description} actions={<></>}>
        {errorMessage ? <>
          <p role="alert" className="text-sm text-red-700">{errorMessage}</p>
          <button type="button" className="demo-button demo-button--quiet mt-6" disabled={isLoading} onClick={() => void reload()}>
            Try again
          </button>
        </> : <p className="text-sm text-gray-600" role="status">Loading question...</p>}
      </QuestionnaireView>
    )
  }

  if (!currentQuestion) {
    return (
      <QuestionnaireView description={description} actions={<></>}>
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
          className="demo-button demo-button--primary mt-6"
        >
          {isCompleting ? 'Submitting...' : 'Submit questionnaire'}
        </button>
      </QuestionnaireView>
    )
  }

  // Remount only when the session or item changes. A passive reset after an
  // async load could otherwise erase the participant's first interaction.
  return (
    <QuestionnaireAnswerForm
      key={JSON.stringify([sessionToken, currentQuestion.itemId])}
      currentQuestion={currentQuestion}
      description={description}
      isLoading={isLoading}
      isSubmitting={isSubmitting}
      isCompleting={isCompleting}
      errorMessage={errorMessage}
      submitAnswer={submitAnswer}
    />
  )
}

type QuestionnaireAnswerFormProps = Pick<UseParticipantQuestionnaireResult,
  'isLoading' | 'isSubmitting' | 'errorMessage' | 'submitAnswer'> & {
  currentQuestion: ParticipantQuestion
  description: string
  isCompleting: boolean
}

// This draft belongs only to the mounted question; the server still determines
// transitions. Rerenders and failed submissions of the same item retain it.
function QuestionnaireAnswerForm({
  currentQuestion,
  description,
  isLoading,
  isSubmitting,
  isCompleting,
  errorMessage,
  submitAnswer,
}: QuestionnaireAnswerFormProps) {
  const [draft, setDraft] = useState<SubmitAnswerPayload>(EMPTY_ANSWER)
  const canSkip = !currentQuestion.required
  const canSubmit = isAnswerPresent(currentQuestion, draft)
  const options = [...currentQuestion.options].sort((a, b) => a.order - b.order)

  const submit = (answer: SubmitAnswerPayload) => {
    void submitAnswer(currentQuestion.itemId, answer)
  }

  return (
    <QuestionnaireView description={description} actions={<></>}>
      <fieldset disabled={isSubmitting || isLoading || isCompleting} aria-busy={isSubmitting}>
      <legend><span>{currentQuestion.position + 1}</span>{currentQuestion.text}</legend>
      <p className="mt-1 text-xs font-medium text-gray-500">{currentQuestion.required ? 'Required' : 'Optional'}</p>

      <div className="mt-4">
        {currentQuestion.questionType === 'SINGLE_CHOICE' && (
          <div className="flex flex-col gap-2">
            {options.map((option) => (
              <label key={option.optionId} className="participation-questionnaire__choice">
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
            {options.map((option) => {
              const selected = draft.optionIds?.includes(option.optionId) ?? false
              return (
                <label key={option.optionId} className="participation-questionnaire__choice">
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
            <div className="demo-scale participation-questionnaire__scale">
              {Array.from(
                {
                  length: (currentQuestion.scaleMax ?? 0) - (currentQuestion.scaleMin ?? 0) + 1,
                },
                (_, index) => (currentQuestion.scaleMin ?? 0) + index,
              ).map((value) => (
                <label
                  key={value}
                >
                  <input type="radio" name={`question-${currentQuestion.itemId}`} aria-label={String(value)} checked={draft.scaleValue === value} onChange={() => setDraft({ ...EMPTY_ANSWER, scaleValue: value })} />
                  <span>{value}</span>
                </label>
              ))}
            </div>
          </div>
        )}

        {currentQuestion.questionType === 'TEXT' && (
          <textarea
            value={draft.textValue ?? ''}
            onChange={(event) => setDraft({ ...EMPTY_ANSWER, textValue: event.target.value })}
            rows={4}
            aria-label={currentQuestion.text}
            placeholder="Your response"
            className="w-full rounded-sm border border-gray-300 px-3 py-2 text-sm"
          />
        )}
      </div>

      </fieldset>
      {errorMessage && (
        <p className="mt-3 text-sm font-semibold text-red-700" role="alert">
          {errorMessage}
        </p>
      )}

      <div className="demo-actions demo-actions--split">
        <button
          type="button"
          disabled={!canSubmit || isSubmitting || isLoading || isCompleting}
          onClick={() => submit(draft)}
          className="demo-button demo-button--primary"
        >
          {isSubmitting ? 'Submitting...' : 'Next'}
        </button>
        {canSkip && (
          <button
            type="button"
            disabled={isSubmitting || isLoading || isCompleting}
            onClick={() => submit({ ...EMPTY_ANSWER, unanswered: true })}
            className="demo-button demo-button--quiet"
          >
            Skip
          </button>
        )}
      </div>
    </QuestionnaireView>
  )
}

export default QuestionnaireStep
