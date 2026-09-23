import { mockDelay } from '../../../shared/api/mockConfig'
import { mockGetQuestionnaire } from '../../../researcher/questionnaire/api/mockQuestionnaire'
import type { QuestionnaireResponse } from '../../../shared/types/questionnaire'
import type { AnswerSubmission, FeedPost, ParticipantSession } from '../types'

const sessions = new Map<string, ParticipantSession>()
let nextSessionSeq = 1

const MOCK_FEED: FeedPost[] = [
  { id: 'post-1', platform: 'X', author: '@新闻速递', content: '突发：市议会批准新公园资金拨款。' },
  { id: 'post-2', platform: 'Instagram', author: '@旅行虫', content: '今晚港口的日落 🌅' },
  { id: 'post-3', platform: 'TikTok', author: '@马可主厨', content: '你一定要试试的三样食材意面小技巧。' },
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
  if (!session) throw new Error('会话不存在。')
  session.consentGivenAt = new Date().toISOString()
  return session
}

export async function mockGetFeed(): Promise<FeedPost[]> {
  await mockDelay()
  // 填充内容 —— 真正的信息流来自 M3，目前还没做。
  return MOCK_FEED
}

// 直接复用研究者模块里内存中的问卷草稿，方便本地演示走通
// "创建 -> 作答 -> 跳转"的完整链路。真实后端在这里应该返回发布时冻结的
// 快照，而不是草稿（见 resolveNextItem.ts 顶部注释）。
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
    throw new Error('会话不存在。')
  }
  // 除了校验会话是否存在外什么都不做：这个 mock 不会把答案持久化到服务端，
  // 在真实接口出现之前，进度由参与端 UI 自己在本地维护（见 SessionContext）。
  void answer
}

export async function mockCompleteSession(sessionId: string): Promise<ParticipantSession> {
  await mockDelay()
  const session = sessions.get(sessionId)
  if (!session) throw new Error('会话不存在。')
  session.status = 'COMPLETED'
  return session
}
