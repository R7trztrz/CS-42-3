// PROPOSED contract for M5 (FR-41~46). SurveyPlatformBackend/docs/m4-api-contract-v1.md
// section 6 is explicit that M4 v1 has no public participant endpoint, so
// every function here stays on the mock unconditionally. Once a real
// participant-session controller exists, replace each mock call below with
// an axios call, the same way researcher/*/api does.
import type { QuestionnaireResponse } from '../../../shared/types/questionnaire'
import type { AnswerSubmission, FeedPost, ParticipantSession } from '../types'
import {
  mockCompleteSession,
  mockCreateSession,
  mockGetFeed,
  mockGetQuestionnaireForSession,
  mockGiveConsent,
  mockSubmitAnswer,
} from './mockSession'

export async function createSession(studyId: string): Promise<ParticipantSession> {
  return mockCreateSession(studyId)
}

export async function giveConsent(sessionId: string): Promise<ParticipantSession> {
  return mockGiveConsent(sessionId)
}

export async function getFeed(): Promise<FeedPost[]> {
  return mockGetFeed()
}

export async function getQuestionnaireForSession(
  studyId: string,
): Promise<QuestionnaireResponse> {
  return mockGetQuestionnaireForSession(studyId)
}

export async function submitAnswer(sessionId: string, answer: AnswerSubmission): Promise<void> {
  return mockSubmitAnswer(sessionId, answer)
}

export async function completeSession(sessionId: string): Promise<ParticipantSession> {
  return mockCompleteSession(sessionId)
}
