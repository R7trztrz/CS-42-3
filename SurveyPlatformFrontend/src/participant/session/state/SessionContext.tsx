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

// UC-32（进度保存与续传）需要一个目前还不存在的服务端接口；localStorage
// 只是客户端临时顶替的方案，方便先把 UI 完整预览一遍，等 FR-45 真正实现后，
// loadProgress/saveProgress 应该换成真实的接口调用。见 sessionApi.ts 顶部注释。
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
    // 尽力而为即可；localStorage 被禁用或写满时不应该导致会话崩溃。
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
          setError(err instanceof Error ? err.message : '会话初始化失败。')
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
    throw new Error('useSession 必须在 SessionProvider 内部使用。')
  }
  return context
}
