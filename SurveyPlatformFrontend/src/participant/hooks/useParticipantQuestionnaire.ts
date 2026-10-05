// M5: participant questionnaire runtime (FR-44).
//
// The server is the only place that computes the next question from a
// branch rule -- this hook never does that locally. It also owns
// Idempotency-Key reuse: retrying the *same* logical answer submission
// must reuse one key, while changing the answer before resubmitting must
// mint a new one (contract section 4.7).

import { useCallback, useEffect, useRef, useState } from 'react'

import {
  getCurrentParticipantQuestion,
  getParticipantErrorCode,
  getParticipantErrorStatus,
  submitParticipantAnswer,
} from '../api/participantApi'
import type { ParticipantQuestion, SubmitAnswerPayload } from '../model/participantSession'

export type UseParticipantQuestionnaireResult = {
  currentQuestion: ParticipantQuestion | null
  readyToSubmit: boolean
  isLoading: boolean
  isSubmitting: boolean
  errorCode: string | null
  errorMessage: string | null
  submitAnswer: (itemId: string, answer: SubmitAnswerPayload) => Promise<boolean>
  reload: () => Promise<void>
}

function newIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }

  // Fallback for environments without crypto.randomUUID (not expected in
  // any supported browser, kept only as a defensive default).
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function describeQuestionnaireError(code: string | null, status: number | null): string {
  switch (code) {
    case 'PARTICIPANT_QUESTIONNAIRE_DISABLED':
      return 'This study does not have a questionnaire.'
    case 'PARTICIPANT_QUESTIONNAIRE_NOT_READY':
      return 'The questionnaire is not available yet.'
    case 'PARTICIPANT_ANSWER_INVALID':
      return 'That answer is not valid for this question.'
    case 'PARTICIPANT_QUESTION_NOT_CURRENT':
      return 'This question has already moved on. Showing the current question.'
    case 'PARTICIPANT_IDEMPOTENCY_CONFLICT':
      return 'That submission is still being processed. Showing the current state.'
    default:
      break
  }

  if (status === 409) {
    return 'This step is out of date. Showing the current state.'
  }

  return 'Something went wrong. Please try again.'
}

export function useParticipantQuestionnaire(
  sessionToken: string | null,
): UseParticipantQuestionnaireResult {
  const [currentQuestion, setCurrentQuestion] = useState<ParticipantQuestion | null>(null)
  const [readyToSubmit, setReadyToSubmit] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorCode, setErrorCode] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // One idempotency key per item, reused while the answer payload is
  // unchanged and replaced the moment it changes.
  const idempotencyKeysRef = useRef(new Map<string, { key: string; signature: string }>())

  const load = useCallback(async () => {
    if (!sessionToken) {
      return
    }

    setIsLoading(true)
    setErrorCode(null)
    setErrorMessage(null)

    try {
      const state = await getCurrentParticipantQuestion(sessionToken)
      setCurrentQuestion(state.currentQuestion)
      setReadyToSubmit(state.readyToSubmit)
    } catch (error) {
      const code = getParticipantErrorCode(error)
      const status = getParticipantErrorStatus(error)
      setErrorCode(code)
      setErrorMessage(describeQuestionnaireError(code, status))
    } finally {
      setIsLoading(false)
    }
  }, [sessionToken])

  useEffect(() => {
    void load()
  }, [load])

  const submitAnswer = useCallback(
    async (itemId: string, answer: SubmitAnswerPayload): Promise<boolean> => {
      if (!sessionToken) {
        return false
      }

      const signature = JSON.stringify(answer)
      const existing = idempotencyKeysRef.current.get(itemId)
      const key =
        existing && existing.signature === signature ? existing.key : newIdempotencyKey()

      idempotencyKeysRef.current.set(itemId, { key, signature })

      setIsSubmitting(true)
      setErrorCode(null)
      setErrorMessage(null)

      try {
        const state = await submitParticipantAnswer(sessionToken, itemId, key, answer)
        setCurrentQuestion(state.currentQuestion)
        setReadyToSubmit(state.readyToSubmit)
        return true
      } catch (error) {
        const code = getParticipantErrorCode(error)
        const status = getParticipantErrorStatus(error)

        // Both of these mean our local view is stale, not that the
        // request should be retried blindly: resync from the server.
        if (code === 'PARTICIPANT_QUESTION_NOT_CURRENT' || code === 'PARTICIPANT_IDEMPOTENCY_CONFLICT') {
          await load()
        }

        setErrorCode(code)
        setErrorMessage(describeQuestionnaireError(code, status))
        return false
      } finally {
        setIsSubmitting(false)
      }
    },
    [sessionToken, load],
  )

  return {
    currentQuestion,
    readyToSubmit,
    isLoading,
    isSubmitting,
    errorCode,
    errorMessage,
    submitAnswer,
    reload: load,
  }
}
