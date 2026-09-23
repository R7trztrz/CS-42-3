// M5（FR-41~46）的*建议*契约 —— 目前还没有真实后端接口，所以每个函数暂时
// 只有 mock 实现。等真正的参与端会话 Controller 出现后，照着
// researcher/*/api 的做法在这里加上 axios 分支，用 USE_MOCK_API 控制切换。
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
    throw new Error('真实的参与端会话接口尚未实现。')
  }
  return mockCreateSession(studyId)
}

export async function giveConsent(sessionId: string): Promise<ParticipantSession> {
  if (!USE_MOCK_API) {
    throw new Error('真实的参与端会话接口尚未实现。')
  }
  return mockGiveConsent(sessionId)
}

export async function getFeed(): Promise<FeedPost[]> {
  if (!USE_MOCK_API) {
    throw new Error('真实的参与端会话接口尚未实现。')
  }
  return mockGetFeed()
}

export async function getQuestionnaireForSession(
  studyId: string,
): Promise<QuestionnaireResponse> {
  if (!USE_MOCK_API) {
    throw new Error('真实的参与端会话接口尚未实现。')
  }
  return mockGetQuestionnaireForSession(studyId)
}

export async function submitAnswer(sessionId: string, answer: AnswerSubmission): Promise<void> {
  if (!USE_MOCK_API) {
    throw new Error('真实的参与端会话接口尚未实现。')
  }
  return mockSubmitAnswer(sessionId, answer)
}

export async function completeSession(sessionId: string): Promise<ParticipantSession> {
  if (!USE_MOCK_API) {
    throw new Error('真实的参与端会话接口尚未实现。')
  }
  return mockCompleteSession(sessionId)
}
