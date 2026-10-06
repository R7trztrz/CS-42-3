import type {
  ConsentDocument,
  CreateParticipantSessionResponse,
  ParticipantQuestion,
  ParticipantSessionPhase,
  ParticipantSessionResponse,
} from '../../src/participant/model/participantSession'

export function deferred<T>() {
  let resolve!: (value: T | PromiseLike<T>) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((fulfil, fail) => {
    resolve = fulfil
    reject = fail
  })
  return { promise, resolve, reject }
}

export function makeConsentDocument(
  overrides: Partial<ConsentDocument> = {},
): ConsentDocument {
  return {
    version: 'platform-default-v1',
    title: 'Informed consent',
    content: 'This is a placeholder consent document.',
    approvedForProduction: false,
    ...overrides,
  }
}

export function makeCreateSessionResponse(
  overrides: Partial<CreateParticipantSessionResponse> = {},
): CreateParticipantSessionResponse {
  return {
    sessionId: 'session-1',
    sessionToken: 'session-token-1',
    status: 'IN_PROGRESS',
    phase: 'CONSENT',
    studyTitle: 'Study title',
    studyDescription: null,
    eyeTrackingEnabled: false,
    questionnaireEnabled: true,
    consentDocument: makeConsentDocument(),
    enteredAt: '2026-10-05T00:00:00Z',
    ...overrides,
  }
}

export function makeSessionResponse(
  phase: ParticipantSessionPhase = 'CONSENT',
  overrides: Partial<ParticipantSessionResponse> = {},
): ParticipantSessionResponse {
  return {
    sessionId: 'session-1',
    status: 'IN_PROGRESS',
    phase,
    abandonmentReason: null,
    currentQuestionItemId: null,
    questionnaireReadyToSubmit: false,
    studyAvailable: true,
    studyTitle: 'Study title',
    studyDescription: null,
    eyeTrackingEnabled: false,
    questionnaireEnabled: true,
    consentDocument: makeConsentDocument(),
    enteredAt: '2026-10-05T00:00:00Z',
    consentedAt: null,
    calibrationCompletedAt: null,
    browsingCompletedAt: null,
    completedAt: null,
    abandonedAt: null,
    lastActivityAt: '2026-10-05T00:00:00Z',
    ...overrides,
  }
}

export function makeQuestion(overrides: Partial<ParticipantQuestion> = {}): ParticipantQuestion {
  return {
    itemId: 'item-1',
    position: 0,
    questionType: 'SINGLE_CHOICE',
    text: 'Do you agree?',
    required: true,
    options: [
      { optionId: 'opt-yes', text: 'Yes', order: 0 },
      { optionId: 'opt-no', text: 'No', order: 1 },
    ],
    scaleMin: null,
    scaleMax: null,
    scaleMinLabel: null,
    scaleMaxLabel: null,
    ...overrides,
  }
}

export function axiosError(status: number, code: string | null) {
  return {
    isAxiosError: true,
    response: {
      status,
      data: { code, error: `error-${status}` },
    },
  }
}
