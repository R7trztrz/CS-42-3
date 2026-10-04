import { mockDelay } from '../../../shared/api/mockConfig'
import type { QuestionResponse } from '../../../shared/types/question'
import type { QuestionnaireResponse } from '../../../shared/types/questionnaire'
import type { AnswerSubmission, FeedPost, ParticipantSession } from '../types'

const sessions = new Map<string, ParticipantSession>()
let nextSessionSeq = 1

const MOCK_FEED: FeedPost[] = [
  { id: 'post-1', platform: 'X', author: '@newsdesk', content: 'Breaking: local council approves new park funding.' },
  { id: 'post-2', platform: 'Instagram', author: '@travelbug', content: 'Sunset over the harbour tonight 🌅' },
  { id: 'post-3', platform: 'TikTok', author: '@chefmarco', content: '3-ingredient pasta hack you need to try.' },
]

// M5 has no backend yet (contract 6 explicitly excludes it - "There is no
// public participant questionnaire/session endpoint in M4 v1"), so this is
// a small self-contained fixture, not a read of the real M4 questionnaire.
// It exists only so the participant flow can be clicked through end to
// end, including one branch jump. Shaped like a PUBLISHED_SNAPSHOT since
// that's what a real M5 backend would hand a participant.
const FIXTURE_QUESTIONS: Record<string, QuestionResponse> = {
  'fixture-q1': {
    questionId: 'fixture-q1',
    type: 'SINGLE_CHOICE',
    questionText: 'How often do you use social media per day?',
    required: true,
    options: [
      { optionId: 'fixture-opt-1', optionText: 'Less than 30 minutes', optionOrder: 0 },
      { optionId: 'fixture-opt-2', optionText: '30 minutes to 2 hours', optionOrder: 1 },
      { optionId: 'fixture-opt-3', optionText: 'More than 2 hours', optionOrder: 2 },
    ],
    scaleMin: null,
    scaleMax: null,
    scaleMinLabel: null,
    scaleMaxLabel: null,
    createdAt: '2026-10-03T00:00:00Z',
    updatedAt: '2026-10-03T00:00:00Z',
  },
  'fixture-q2': {
    questionId: 'fixture-q2',
    type: 'TEXT',
    questionText: "Tell us more about what you'd cut back on.",
    required: false,
    options: [],
    scaleMin: null,
    scaleMax: null,
    scaleMinLabel: null,
    scaleMaxLabel: null,
    createdAt: '2026-10-03T00:00:00Z',
    updatedAt: '2026-10-03T00:00:00Z',
  },
  'fixture-q3': {
    questionId: 'fixture-q3',
    type: 'SCALE',
    questionText: 'How trustworthy did this feed feel overall?',
    required: true,
    options: [],
    scaleMin: 1,
    scaleMax: 10,
    scaleMinLabel: 'Not trustworthy at all',
    scaleMaxLabel: 'Extremely trustworthy',
    createdAt: '2026-10-03T00:00:00Z',
    updatedAt: '2026-10-03T00:00:00Z',
  },
}

function buildFixtureQuestionnaire(studyId: string): QuestionnaireResponse {
  return {
    questionnaireId: 'fixture-questionnaire',
    snapshotId: 'fixture-snapshot',
    studyId,
    contentSource: 'PUBLISHED_SNAPSHOT',
    items: [
      {
        itemId: 'fixture-item-1',
        position: 0,
        missing: false,
        question: FIXTURE_QUESTIONS['fixture-q1'],
        referenceStatus: 'VALID',
        branchRules: [
          // Answering "More than 2 hours" skips the quick-use follow-up
          // and jumps straight to the trust question.
          {
            id: 'fixture-rule-1',
            sourceOptionId: 'fixture-opt-3',
            sourceScaleValue: null,
            targetItemId: 'fixture-item-3',
            targetPosition: 2,
          },
        ],
        defaultNextItemId: 'fixture-item-2',
      },
      {
        itemId: 'fixture-item-2',
        position: 1,
        missing: false,
        question: FIXTURE_QUESTIONS['fixture-q2'],
        referenceStatus: 'VALID',
        branchRules: [],
        defaultNextItemId: 'fixture-item-3',
      },
      {
        itemId: 'fixture-item-3',
        position: 2,
        missing: false,
        question: FIXTURE_QUESTIONS['fixture-q3'],
        referenceStatus: 'VALID',
        branchRules: [],
        defaultNextItemId: null,
      },
    ],
    version: 1,
    updatedAt: '2026-10-03T00:00:00Z',
    publishedAt: '2026-10-03T00:00:00Z',
    valid: true,
    validationIssues: [],
  }
}

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

export async function mockGetQuestionnaireForSession(
  studyId: string,
): Promise<QuestionnaireResponse> {
  await mockDelay()
  return buildFixtureQuestionnaire(studyId)
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
