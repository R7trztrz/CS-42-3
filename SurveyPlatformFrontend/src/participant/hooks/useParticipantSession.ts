// M5: session creation and restoration for the participant flow.
//
// This hook owns exactly the pieces the M5 handoff doc calls out as the
// current gap -- creating an anonymous session, persisting its token,
// restoring it on reload, and distinguishing recoverable errors from a
// closed Study. It deliberately does not render anything or decide what UI
// a given `phase` shows; that belongs to the per-phase step components
// built on top of it (ConsentStep, BrowsingStep, ...).

import { useCallback, useEffect, useRef, useState } from 'react'

import {
  abandonParticipantSession,
  completeParticipantBrowsing,
  createParticipantSession,
  decideParticipantConsent,
  getCurrentParticipantSession,
  getParticipantErrorCode,
  getParticipantErrorStatus,
  submitParticipantQuestionnaire,
} from '../api/participantApi'
import {
  clearStoredSessionToken,
  readStoredSessionToken,
  storeSessionToken,
  type ParticipantDeviceInfo,
  type ParticipantSessionResponse,
} from '../model/participantSession'

export type ParticipantSessionStage =
  // Checking sessionStorage / restoring a previously created session.
  | 'checking'
  // No usable session; waiting for the participant to start one.
  | 'landing'
  // POST .../sessions in flight.
  | 'creating'
  // A session was created or restored; `session` is populated.
  | 'active'
  // 410 STUDY_CLOSED: terminal, no session can be created or continued.
  | 'closed'
  // Restore or create failed for a recoverable reason (network, 5xx, ...).
  | 'error'

export type UseParticipantSessionResult = {
  stage: ParticipantSessionStage
  session: ParticipantSessionResponse | null
  sessionToken: string | null
  errorCode: string | null
  errorMessage: string | null
  createSession: (deviceInfo?: ParticipantDeviceInfo | null) => Promise<void>
  retry: () => Promise<void>
  // Mutating actions against the *existing* active session (consent,
  // browsing completion, explicit exit). Each returns whether it
  // succeeded; failures are surfaced through actionError* below rather
  // than resetting `stage`, since the bootstrap session is still valid.
  decideConsent: (accepted: boolean) => Promise<boolean>
  completeBrowsing: () => Promise<boolean>
  completeQuestionnaire: () => Promise<boolean>
  abandon: () => Promise<boolean>
  refreshSession: () => Promise<boolean>
  isActionPending: boolean
  actionErrorCode: string | null
  actionErrorMessage: string | null
  clearActionError: () => void
}

function describeError(code: string | null, status: number | null): string {
  switch (code) {
    case 'PARTICIPATION_NOT_FOUND':
      return 'This participation link is not valid.'
    case 'STUDY_CLOSED':
      return 'This study is closed and is no longer accepting participants.'
    case 'FEED_NOT_READY':
      return 'This study is not ready to accept participants yet.'
    case 'PARTICIPANT_QUESTIONNAIRE_NOT_READY':
      return 'This study is not ready to accept participants yet.'
    default:
      break
  }

  if (status === 401 || status === 404) {
    return 'Your session could not be found. Please start again.'
  }

  return 'Something went wrong. Please try again.'
}

export function useParticipantSession(studyToken: string | undefined): UseParticipantSessionResult {
  const [stage, setStage] = useState<ParticipantSessionStage>('checking')
  const [session, setSession] = useState<ParticipantSessionResponse | null>(null)
  const [sessionToken, setSessionTokenState] = useState<string | null>(null)
  const setSessionToken = useCallback((token: string | null) => {
    sessionTokenRef.current = token
    setSessionTokenState(token)
  }, [])
  const [errorCode, setErrorCode] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [isActionPending, setIsActionPending] = useState(false)
  const [actionErrorCode, setActionErrorCode] = useState<string | null>(null)
  const [actionErrorMessage, setActionErrorMessage] = useState<string | null>(null)

  // Guards state updates from a restore/create call that is still in
  // flight when the component unmounts or studyToken changes.
  const activeRequestRef = useRef(0)
  // The current session token, readable synchronously by action callbacks
  // without adding `sessionToken` state to their dependency arrays.
  const sessionTokenRef = useRef<string | null>(null)

  const restore = useCallback(
    async (token: string) => {
      if (!studyToken) {
        return
      }

      const requestId = ++activeRequestRef.current

      try {
        const current = await getCurrentParticipantSession(token)

        if (activeRequestRef.current !== requestId) {
          return
        }

        setSessionToken(token)
        setSession(current)
        setErrorCode(null)
        setErrorMessage(null)
        setStage('active')
      } catch (error) {
        if (activeRequestRef.current !== requestId) {
          return
        }

        const code = getParticipantErrorCode(error)
        const status = getParticipantErrorStatus(error)

        if (status === 410 || code === 'STUDY_CLOSED') {
          setErrorCode(code)
          setErrorMessage(describeError(code, status))
          setStage('closed')
          return
        }

        if (
          status === 401 ||
          status === 404 ||
          code === 'PARTICIPANT_SESSION_UNAUTHORIZED' ||
          code === 'PARTICIPANT_SESSION_NOT_FOUND'
        ) {
          // Stale or foreign token: drop it and let the participant start
          // fresh. This must never redirect to the researcher login.
          clearStoredSessionToken(studyToken)
          setSessionToken(null)
          setSession(null)
          setErrorCode(null)
          setErrorMessage(null)
          setStage('landing')
          return
        }

        setErrorCode(code)
        setErrorMessage(describeError(code, status))
        setStage('error')
      }
    },
    [studyToken],
  )

  useEffect(() => {
    if (!studyToken) {
      setStage('error')
      setErrorCode(null)
      setErrorMessage('This participation link is not valid.')
      return
    }

    setStage('checking')

    const storedToken = readStoredSessionToken(studyToken)

    if (!storedToken) {
      setStage('landing')
      return
    }

    void restore(storedToken)
  }, [studyToken, restore])

  const createSession = useCallback(
    async (deviceInfo: ParticipantDeviceInfo | null = null) => {
      if (!studyToken || stage === 'checking' || stage === 'creating' || stage === 'active') {
        return
      }

      const requestId = ++activeRequestRef.current
      setStage('creating')
      setErrorCode(null)
      setErrorMessage(null)

      try {
        const created = await createParticipantSession(studyToken, deviceInfo)

        if (activeRequestRef.current !== requestId) {
          return
        }

        storeSessionToken(studyToken, created.sessionToken)
        // Re-fetch through the canonical restore endpoint so every
        // consumer downstream works with one session shape, regardless of
        // whether it came from create or restore.
        await restore(created.sessionToken)
      } catch (error) {
        if (activeRequestRef.current !== requestId) {
          return
        }

        const code = getParticipantErrorCode(error)
        const status = getParticipantErrorStatus(error)

        if (status === 410 || code === 'STUDY_CLOSED') {
          setErrorCode(code)
          setErrorMessage(describeError(code, status))
          setStage('closed')
          return
        }

        setErrorCode(code)
        setErrorMessage(describeError(code, status))
        setStage('error')
      }
    },
    [studyToken, stage, restore],
  )

  const retry = useCallback(async () => {
    if (!studyToken) {
      return
    }

    const storedToken = readStoredSessionToken(studyToken)

    if (storedToken) {
      setStage('checking')
      await restore(storedToken)
      return
    }

    setStage('landing')
    setErrorCode(null)
    setErrorMessage(null)
  }, [studyToken, restore])

  // Shared error handling for every action that mutates an *existing*
  // session (consent, browsing completion, abandonment). A session that
  // dies mid-flow (401/404) returns to landing; a Study that closes
  // becomes the closed terminal stage; everything else is a recoverable
  // actionError the current step component can show inline and retry.
  const applySessionAction = useCallback(
    async (action: (token: string) => Promise<ParticipantSessionResponse>): Promise<boolean> => {
      const token = sessionTokenRef.current

      if (!studyToken || !token) {
        return false
      }

      setIsActionPending(true)
      setActionErrorCode(null)
      setActionErrorMessage(null)

      try {
        const updated = await action(token)
        setSession(updated)
        return true
      } catch (error) {
        const code = getParticipantErrorCode(error)
        const status = getParticipantErrorStatus(error)

        if (status === 410 || code === 'STUDY_CLOSED') {
          setErrorCode(code)
          setErrorMessage(describeError(code, status))
          setStage('closed')
          return false
        }

        if (
          status === 401 ||
          status === 404 ||
          code === 'PARTICIPANT_SESSION_UNAUTHORIZED' ||
          code === 'PARTICIPANT_SESSION_NOT_FOUND'
        ) {
          clearStoredSessionToken(studyToken)
          setSessionToken(null)
          setSession(null)
          setStage('landing')
          return false
        }

        setActionErrorCode(code)
        setActionErrorMessage(describeError(code, status))
        return false
      } finally {
        setIsActionPending(false)
      }
    },
    [studyToken, setSessionToken],
  )

  const decideConsent = useCallback(
    (accepted: boolean) => applySessionAction((token) => decideParticipantConsent(token, accepted)),
    [applySessionAction],
  )

  const completeBrowsing = useCallback(
    () => applySessionAction((token) => completeParticipantBrowsing(token)),
    [applySessionAction],
  )

  const completeQuestionnaire = useCallback(
    () => applySessionAction((token) => submitParticipantQuestionnaire(token)),
    [applySessionAction],
  )

  const abandon = useCallback(
    () => applySessionAction((token) => abandonParticipantSession(token)),
    [applySessionAction],
  )

  const refreshSession = useCallback(
    () => applySessionAction((token) => getCurrentParticipantSession(token)),
    [applySessionAction],
  )

  const clearActionError = useCallback(() => {
    setActionErrorCode(null)
    setActionErrorMessage(null)
  }, [])

  return {
    stage,
    session,
    sessionToken,
    errorCode,
    errorMessage,
    createSession,
    retry,
    decideConsent,
    completeBrowsing,
    completeQuestionnaire,
    abandon,
    refreshSession,
    isActionPending,
    actionErrorCode,
    actionErrorMessage,
    clearActionError,
  }
}
