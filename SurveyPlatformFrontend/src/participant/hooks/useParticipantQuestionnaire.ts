// M5: participant questionnaire runtime (FR-44).
//
// The server is the only place that computes the next question from a
// branch rule -- this hook never does that locally. It also owns
// Idempotency-Key reuse: retrying the *same* logical answer submission
// must reuse one key, while changing the answer before resubmitting must
// mint a new one (contract section 4.7).

import { useCallback, useEffect, useMemo, useRef, useState } from 'react'

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
  const sessionScope = useMemo(() => ({ sessionToken }), [sessionToken])
  const currentScopeRef = useRef<typeof sessionScope | null>(null)
  const activeRequestRef = useRef(0)
  const submissionRequestRef = useRef(0)
  const submissionInFlightRef = useRef(false)
  const [currentQuestion, setCurrentQuestion] = useState<ParticipantQuestion | null>(null)
  const [readyToSubmit, setReadyToSubmit] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorCode, setErrorCode] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // One idempotency key per item, reused while the answer payload is
  // unchanged and replaced the moment it changes.
  const idempotencyKeysRef = useRef(new Map<string, { key: string; signature: string }>())
  const isCurrentSession = useCallback(
    () => currentScopeRef.current === sessionScope,
    [sessionScope],
  )

  useEffect(() => {
    currentScopeRef.current = sessionScope
    ++activeRequestRef.current
    ++submissionRequestRef.current
    submissionInFlightRef.current = false
    idempotencyKeysRef.current.clear()
    setCurrentQuestion(null)
    setReadyToSubmit(false)
    setIsLoading(false)
    setIsSubmitting(false)
    setErrorCode(null)
    setErrorMessage(null)
    return () => {
      currentScopeRef.current = null
      ++activeRequestRef.current
      ++submissionRequestRef.current
      submissionInFlightRef.current = false
      idempotencyKeysRef.current.clear()
    }
  }, [sessionScope])

  const load = useCallback(async (duringSubmission = false) => {
    if (!sessionToken || !isCurrentSession() || (submissionInFlightRef.current && !duringSubmission)) {
      return
    }

    const requestId = ++activeRequestRef.current
    const isCurrentRequest = () => isCurrentSession() && activeRequestRef.current === requestId
    setIsLoading(true)
    setErrorCode(null)
    setErrorMessage(null)

    try {
      const state = await getCurrentParticipantQuestion(sessionToken)
      if (!isCurrentRequest()) return
      setCurrentQuestion(state.currentQuestion)
      setReadyToSubmit(state.readyToSubmit)
    } catch (error) {
      if (!isCurrentRequest()) return
      const code = getParticipantErrorCode(error)
      const status = getParticipantErrorStatus(error)
      setErrorCode(code)
      setErrorMessage(describeQuestionnaireError(code, status))
    } finally {
      if (isCurrentRequest()) setIsLoading(false)
    }
  }, [sessionToken, isCurrentSession])

  useEffect(() => {
    void load()
  }, [load])

  const submitAnswer = useCallback(
    async (itemId: string, answer: SubmitAnswerPayload): Promise<boolean> => {
      if (!sessionToken || !isCurrentSession() || submissionInFlightRef.current) {
        return false
      }

      const requestId = ++activeRequestRef.current
      const submissionId = ++submissionRequestRef.current
      const isCurrentSubmission = () => isCurrentSession() && submissionRequestRef.current === submissionId
      const isCurrentRequest = () => isCurrentSubmission() && activeRequestRef.current === requestId
      submissionInFlightRef.current = true
      const signature = JSON.stringify(answer)
      const existing = idempotencyKeysRef.current.get(itemId)
      const key =
        existing && existing.signature === signature ? existing.key : newIdempotencyKey()

      idempotencyKeysRef.current.set(itemId, { key, signature })

      setIsLoading(false)
      setIsSubmitting(true)
      setErrorCode(null)
      setErrorMessage(null)

      try {
        const state = await submitParticipantAnswer(sessionToken, itemId, key, answer)
        if (!isCurrentRequest()) return false
        setCurrentQuestion(state.currentQuestion)
        setReadyToSubmit(state.readyToSubmit)
        return true
      } catch (error) {
        if (!isCurrentRequest()) return false
        const code = getParticipantErrorCode(error)
        const status = getParticipantErrorStatus(error)

        // Both of these mean our local view is stale, not that the
        // request should be retried blindly: resync from the server.
        if (code === 'PARTICIPANT_QUESTION_NOT_CURRENT' || code === 'PARTICIPANT_IDEMPOTENCY_CONFLICT') {
          await load(true)
          if (!isCurrentSubmission()) return false
        }

        setErrorCode(code)
        setErrorMessage(describeQuestionnaireError(code, status))
        return false
      } finally {
        if (isCurrentSubmission()) {
          submissionInFlightRef.current = false
          setIsSubmitting(false)
        }
      }
    },
    [sessionToken, load, isCurrentSession],
  )

  const reload = useCallback(() => load(), [load])

  return {
    currentQuestion,
    readyToSubmit,
    isLoading,
    isSubmitting,
    errorCode,
    errorMessage,
    submitAnswer,
    reload,
  }
}
