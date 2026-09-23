// M5's backend doesn't exist yet (see SurveyPlatformBackend/docs, FR-41~46
// are unimplemented), so this is a *proposed* contract, not a confirmed
// one. Treat it as a starting point to reconcile with whoever builds the
// participant-session backend, not as something to build against blindly.

export type SessionStatus = 'ACTIVE' | 'COMPLETED' | 'ABANDONED'

export interface ParticipantSession {
  id: string
  studyId: string
  status: SessionStatus
  consentGivenAt: string | null
  createdAt: string
}

// Placeholder content shape for UC-30 (browse simulated feed). M3 (the
// feed/content editor) isn't built yet, so this is generic filler, not a
// contract to match against the real feed authoring format.
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

// What the participant client persists locally so a page refresh resumes
// mid-session (a client-side stand-in for FR-45's server-side resume until
// that endpoint exists).
export interface SessionProgress {
  sessionId: string
  studyId: string
  consentGivenAt: string | null
  hasSeenFeed: boolean
  currentItemId: string | null
  answeredItemIds: string[]
}
