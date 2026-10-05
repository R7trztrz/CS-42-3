// M5 participant session types and local storage helpers.
//
// Contract source: M5前端联调交接说明_2026-10-05.md, sections 2-5, matching the
// backend records in SurveyPlatformBackend/.../participation/api/dto and
// .../participation/domain (ParticipantSession* enums).

export type ParticipantSessionStatus = 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED'

export type ParticipantSessionPhase =
  | 'CONSENT'
  | 'CALIBRATION'
  | 'BROWSING'
  | 'QUESTIONNAIRE'
  | 'FINISHED'

export type ParticipantAbandonmentReason =
  | 'CONSENT_DECLINED'
  | 'PARTICIPANT_EXIT'
  | 'INACTIVITY_TIMEOUT'
  | 'STUDY_CLOSED'
  | null

export type ConsentDocument = {
  version: string
  title: string
  content: string
  approvedForProduction: boolean
}

export type ParticipantDeviceInfo = {
  browser?: string
  browserVersion?: string
  os?: string
  screenWidth?: number
  screenHeight?: number
  timezone?: string
}

// Response to POST /api/participation/{studyToken}/sessions.
// `sessionToken` is returned exactly once; it is never present on the
// restore response below.
export type CreateParticipantSessionResponse = {
  sessionId: string
  sessionToken: string
  status: ParticipantSessionStatus
  phase: ParticipantSessionPhase
  studyTitle: string
  studyDescription: string | null
  eyeTrackingEnabled: boolean
  questionnaireEnabled: boolean
  consentDocument: ConsentDocument
  enteredAt: string
}

// Response to GET /api/participant-session (session restore). This is the
// authoritative state for every phase after creation; the frontend must not
// infer phase/status from local page history.
export type ParticipantSessionResponse = {
  sessionId: string
  status: ParticipantSessionStatus
  phase: ParticipantSessionPhase
  abandonmentReason: ParticipantAbandonmentReason
  currentQuestionItemId: string | null
  questionnaireReadyToSubmit: boolean
  studyAvailable: boolean
  studyTitle: string
  studyDescription: string | null
  eyeTrackingEnabled: boolean
  questionnaireEnabled: boolean
  consentDocument: ConsentDocument
  enteredAt: string
  consentedAt: string | null
  calibrationCompletedAt: string | null
  browsingCompletedAt: string | null
  completedAt: string | null
  abandonedAt: string | null
  lastActivityAt: string
}

// M5 error envelope: { code, error }. The frontend must branch on `code`
// only (see contract section 5) -- `error` is an English message for logs.
export type ParticipantErrorResponse = {
  code: string | null
  error: string
}

export type ParticipantQuestionType = 'SINGLE_CHOICE' | 'MULTI_CHOICE' | 'SCALE' | 'TEXT'

export type ParticipantQuestionOption = {
  optionId: string
  text: string
  order: number
}

// Response to GET /api/participant-session/questionnaire/current and to a
// successful answer PUT. `currentQuestion` is null once the branch path has
// reached END, at which point `readyToSubmit` is true.
export type ParticipantQuestion = {
  itemId: string
  position: number
  questionType: ParticipantQuestionType
  text: string
  required: boolean
  options: ParticipantQuestionOption[]
  scaleMin: number | null
  scaleMax: number | null
  scaleMinLabel: string | null
  scaleMaxLabel: string | null
}

export type ParticipantQuestionnaireState = {
  currentQuestion: ParticipantQuestion | null
  readyToSubmit: boolean
}

// Exactly one of these fields is meaningful per question type; the rest
// stay null. `unanswered: true` skips an optional question and must be the
// only populated field in that case.
export type SubmitAnswerPayload = {
  optionId: string | null
  optionIds: string[] | null
  scaleValue: number | null
  textValue: string | null
  unanswered: boolean
}

// Every M5 code this frontend is required to branch on (contract section 5).
export type ParticipantErrorCode =
  | 'PARTICIPATION_NOT_FOUND'
  | 'STUDY_CLOSED'
  | 'FEED_NOT_READY'
  | 'PARTICIPANT_SESSION_UNAUTHORIZED'
  | 'PARTICIPANT_SESSION_NOT_FOUND'
  | 'PARTICIPANT_SESSION_STATE_INVALID'
  | 'PARTICIPANT_SESSION_TERMINATED'
  | 'PARTICIPANT_IDEMPOTENCY_CONFLICT'
  | 'PARTICIPANT_QUESTION_NOT_CURRENT'
  | 'PARTICIPANT_ANSWER_INVALID'
  | 'PARTICIPANT_QUESTIONNAIRE_DISABLED'
  | 'PARTICIPANT_QUESTIONNAIRE_NOT_READY'
  | 'REQUEST_BODY_INVALID'
  | 'REQUEST_VALIDATION_FAILED'
  | 'ARGUMENT_TYPE_MISMATCH'
  | 'REQUEST_FAILED'

export const PARTICIPANT_SESSION_TOKEN_HEADER = 'X-Participant-Session-Token'

function storageKey(studyToken: string) {
  return `participant-session:${studyToken}`
}

// All sessionStorage access is wrapped: private browsing, blocked storage
// and non-browser test environments can throw or be unavailable.
export function readStoredSessionToken(studyToken: string): string | null {
  try {
    return window.sessionStorage.getItem(storageKey(studyToken))
  } catch {
    return null
  }
}

export function storeSessionToken(studyToken: string, sessionToken: string): void {
  try {
    window.sessionStorage.setItem(storageKey(studyToken), sessionToken)
  } catch {
    // Best-effort only; refreshing the page will just start a new session.
  }
}

export function clearStoredSessionToken(studyToken: string): void {
  try {
    window.sessionStorage.removeItem(storageKey(studyToken))
  } catch {
    // Nothing to clean up if storage is unavailable.
  }
}
