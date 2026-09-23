import { mockDelay } from '../../../shared/api/mockConfig'
import { mockGetQuestionnaire } from '../../../researcher/questionnaire/api/mockQuestionnaire'
import type { QuestionnaireResponse } from '../../../shared/types/questionnaire'
import type { AnswerSubmission, FeedPost, ParticipantSession } from '../types'

const sessions = new Map<string, ParticipantSession>()
let nextSessionSeq = 1

const MOCK_FEED: FeedPost[] = [
  { id: 'post-1', platform: 'X', author: '@newsdesk', content: 'Breaking: local council approves new park funding.' },
  { id: 'post-2', platform: 'Instagram', author: '@travelbug', content: 'Sunset over the harbour tonight 🌅' },
  { id: 'post-3', platform: 'TikTok', author: '@chefmarco', content: '3-ingredient pasta hack you need to try.' },
]

export async function mockCreateSession(studyId: string): Promise<ParticipantSession> {
  await mockDelay()
  const session: ParticipantSession = {
    id: `mock-session-${nextSessionSeq++}`,
    studyId,
    status: 'ACTIVE',
    consentGivenAt: null,
    createdAt: new Date().toISOString(),
  }
  sessions.set(session.id, session)
  return session
}

export async function mockGiveConsent(sessionId: string): Promise<ParticipantSession> {
  await mockDelay()
  const session = sessions.get(sessionId)
  if (!session) throw new Error('Session not found.')
  session.consentGivenAt = new Date().toISOString()
  return session
}

export async function mockGetFeed(): Promise<FeedPost[]> {
  await mockDelay()
  // Filler content - the real feed comes from M3, which isn't built yet.
  return MOCK_FEED
}

// Reuses the researcher module's in-memory questionnaire draft so a local
// demo can exercise create -> answer -> branch end-to-end. A real backend
// would serve a frozen publish snapshot here instead (see
// resolveNextItem.ts's header comment).
export async function mockGetQuestionnaireForSession(
  studyId: string,
): Promise<QuestionnaireResponse> {
  await mockDelay()
  return mockGetQuestionnaire(studyId)
}

export async function mockSubmitAnswer(
  sessionId: string,
  answer: AnswerSubmission,
): Promise<void> {
  await mockDelay(150)
  if (!sessions.has(sessionId)) {
    throw new Error('Session not found.')
  }
  // No-op beyond validating the session exists: this mock doesn't persist
  // answers server-side, the participant UI tracks progress locally
  // (see SessionContext) until a real endpoint exists.
  void answer
}

export async function mockCompleteSession(sessionId: string): Promise<ParticipantSession> {
  await mockDelay()
  const session = sessions.get(sessionId)
  if (!session) throw new Error('Session not found.')
  session.status = 'COMPLETED'
  return session
}
