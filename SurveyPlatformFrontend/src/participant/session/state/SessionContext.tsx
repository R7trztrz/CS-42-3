import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { resolveNextItem } from '../logic/resolveNextItem'
import {
  completeSession,
  createSession,
  getFeed,
  getQuestionnaireForSession,
  giveConsent,
  submitAnswer,
} from '../api/sessionApi'
import type { QuestionnaireItemResponse, QuestionnaireResponse } from '../../../shared/types/questionnaire'
import type { AnswerSubmission, FeedPost, ParticipantSession, SessionProgress } from '../types'

function progressKey(studyId: string) {
  return `survey-session-progress:${studyId}`
}

// UC-32 (progress save & resume) needs a server-side endpoint that doesn't
// exist yet; localStorage is a client-only stand-in so the UI can be
// previewed end-to-end, and a real API call should replace loadProgress/
// saveProgress once FR-45 is implemented. See sessionApi.ts's header.
function loadProgress(studyId: string): SessionProgress | null {
  try {
    const raw = window.localStorage.getItem(progressKey(studyId))
    return raw ? (JSON.parse(raw) as SessionProgress) : null
  } catch {
    return null
  }
}

function saveProgress(progress: SessionProgress) {
  try {
    window.localStorage.setItem(progressKey(progress.studyId), JSON.stringify(progress))
  } catch {
    // Best-effort only; a blocked/full localStorage shouldn't crash the session.
  }
}

interface SessionContextValue {
  session: ParticipantSession | null
  questionnaire: QuestionnaireResponse | null
  feed: FeedPost[]
  currentItem: QuestionnaireItemResponse | null
  hasConsented: boolean
  hasSeenFeed: boolean
  isComplete: boolean
  isLoading: boolean
  error: string
  recordConsent: () => Promise<void>
  markFeedSeen: () => void
  submitCurrentAnswer: (answer: Omit<AnswerSubmission, 'itemId'>) => Promise<void>
}

const SessionContext = createContext<SessionContextValue | null>(null)

export function SessionProvider({ studyId, children }: { studyId: string; children: ReactNode }) {
  const [session, setSession] = useState<ParticipantSession | null>(null)
  const [questionnaire, setQuestionnaire] = useState<QuestionnaireResponse | null>(null)
  const [feed, setFeed] = useState<FeedPost[]>([])
  const [progress, setProgress] = useState<SessionProgress | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    async function init() {
      try {
        const [newSession, questionnaireResponse, feedPosts] = await Promise.all([
          createSession(studyId),
          getQuestionnaireForSession(studyId),
          getFeed(),
        ])
        if (cancelled) return

        const existing = loadProgress(studyId)
        const resumed: SessionProgress = existing ?? {
          sessionId: newSession.id,
          studyId,
          consentGivenAt: null,
          hasSeenFeed: false,
          currentItemId: questionnaireResponse.items[0]?.id ?? null,
          answeredItemIds: [],
        }

        setSession(newSession)
        setQuestionnaire(questionnaireResponse)
        setFeed(feedPosts)
        setProgress(resumed)
        saveProgress(resumed)
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to start session.')
        }
      } finally {
        if (!cancelled) setIsLoading(false)
      }
    }

    init()
    return () => {
      cancelled = true
    }
  }, [studyId])

  const currentItem = useMemo(() => {
    if (!questionnaire || !progress?.currentItemId) return null
    return questionnaire.items.find((item) => item.id === progress.currentItemId) ?? null
  }, [questionnaire, progress])

  const recordConsent = useCallback(async () => {
    if (!session) return
    await giveConsent(session.id)
    setProgress((prev) => {
      if (!prev) return prev
      const next = { ...prev, consentGivenAt: new Date().toISOString() }
      saveProgress(next)
      return next
    })
  }, [session])

  const markFeedSeen = useCallback(() => {
    setProgress((prev) => {
      if (!prev) return prev
      const next = { ...prev, hasSeenFeed: true }
      saveProgress(next)
      return next
    })
  }, [])

  const submitCurrentAnswer = useCallback(
    async (answer: Omit<AnswerSubmission, 'itemId'>) => {
      if (!session || !questionnaire || !currentItem) return

      await submitAnswer(session.id, { itemId: currentItem.id, ...answer })

      const next = resolveNextItem(questionnaire.items, currentItem, answer)
      setProgress((prev) => {
        if (!prev) return prev
        const updated: SessionProgress = {
          ...prev,
          currentItemId: next ? next.id : null,
          answeredItemIds: [...prev.answeredItemIds, currentItem.id],
        }
        saveProgress(updated)
        return updated
      })

      if (!next) {
        await completeSession(session.id)
      }
    },
    [session, questionnaire, currentItem],
  )

  const value: SessionContextValue = {
    session,
    questionnaire,
    feed,
    currentItem,
    hasConsented: Boolean(progress?.consentGivenAt),
    hasSeenFeed: Boolean(progress?.hasSeenFeed),
    isComplete: Boolean(progress && !progress.currentItemId && progress.answeredItemIds.length > 0),
    isLoading,
    error,
    recordConsent,
    markFeedSeen,
    submitCurrentAnswer,
  }

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}

export function useSession(): SessionContextValue {
  const context = useContext(SessionContext)
  if (!context) {
    throw new Error('useSession must be used within a SessionProvider.')
  }
  return context
}
