// M5 的后端还没有实现（见 SurveyPlatformBackend/docs，FR-41~46 尚未落地），
// 所以这只是一份*建议*契约，不是已经确认的接口。可以作为和后续负责搭建
// 参与端会话后端的同学对齐的起点，不要直接当成既定接口去死磕。

export type SessionStatus = 'ACTIVE' | 'COMPLETED' | 'ABANDONED'

export interface ParticipantSession {
  id: string
  studyId: string
  status: SessionStatus
  consentGivenAt: string | null
  createdAt: string
}

// UC-30（浏览模拟信息流）的占位内容结构。M3（信息流编辑模块）还没做，
// 这里只是通用的填充数据，不是要对齐的正式信息流格式。
export interface FeedPost {
  id: string
  platform: string
  author: string
  content: string
}

export interface AnswerSubmission {
  itemId: string
  selectedOptionId?: string
  selectedOptionIds?: string[]
  scaleValue?: number
  textAnswer?: string
}

// 参与端本地持久化的进度信息，用于页面刷新后能恢复到之前的进度
// （在真正的 FR-45 服务端续传接口出现之前，先用客户端方案顶替）。
export interface SessionProgress {
  sessionId: string
  studyId: string
  consentGivenAt: string | null
  hasSeenFeed: boolean
  currentItemId: string | null
  answeredItemIds: string[]
}
