// PROPOSED contract for M5 (FR-41~46) - no backend endpoint exists yet, so
// every function only has a mock implementation for now. Once a real
// participant-session controller exists, add the axios branch here the
// same way researcher/*/api does, gated by USE_MOCK_API.
import { USE_MOCK_API } from '../../../shared/api/mockConfig'
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
  if (!USE_MOCK_API) {
    throw new Error('Real participant-session API is not implemented yet.')
  }
  return mockCreateSession(studyId)
}

export async function giveConsent(sessionId: string): Promise<ParticipantSession> {
  if (!USE_MOCK_API) {
    throw new Error('Real participant-session API is not implemented yet.')
  }
  return mockGiveConsent(sessionId)
}

export async function getFeed(): Promise<FeedPost[]> {
  if (!USE_MOCK_API) {
    throw new Error('Real participant-session API is not implemented yet.')
  }
  return mockGetFeed()
}

export async function getQuestionnaireForSession(
  studyId: string,
): Promise<QuestionnaireResponse> {
  if (!USE_MOCK_API) {
    throw new Error('Real participant-session API is not implemented yet.')
  }
  return mockGetQuestionnaireForSession(studyId)
}

export async function submitAnswer(sessionId: string, answer: AnswerSubmission): Promise<void> {
  if (!USE_MOCK_API) {
    throw new Error('Real participant-session API is not implemented yet.')
  }
  return mockSubmitAnswer(sessionId, answer)
}

export async function completeSession(sessionId: string): Promise<ParticipantSession> {
  if (!USE_MOCK_API) {
    throw new Error('Real participant-session API is not implemented yet.')
  }
  return mockCompleteSession(sessionId)
}
