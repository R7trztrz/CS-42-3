import { beforeEach, describe, expect, it, vi } from 'vitest'

const { httpClientMock } = vi.hoisted(() => ({
  httpClientMock: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

vi.mock('../../src/participant/api/participantHttpClient', () => ({
  default: httpClientMock,
}))

import {
  abandonParticipantSession,
  completeParticipantBrowsing,
  createParticipantSession,
  decideParticipantConsent,
  getCurrentParticipantQuestion,
  getCurrentParticipantSession,
  getParticipantErrorCode,
  getParticipantErrorStatus,
  submitParticipantAnswer,
  submitParticipantQuestionnaire,
} from '../../src/participant/api/participantApi'
import { PARTICIPANT_SESSION_TOKEN_HEADER } from '../../src/participant/model/participantSession'
import {
  axiosError,
  makeCreateSessionResponse,
  makeQuestion,
  makeSessionResponse,
} from './fixtures'

describe('M5 participant API client', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('creates a session against the study-token path, not the session-token header', async () => {
    const created = makeCreateSessionResponse()
    httpClientMock.post.mockResolvedValue({ data: created })

    const result = await createParticipantSession('study-token-xyz', { browser: 'Chrome' })

    expect(httpClientMock.post).toHaveBeenCalledWith(
      '/api/participation/study-token-xyz/sessions',
      { deviceInfo: { browser: 'Chrome' } },
    )
    expect(result).toEqual(created)
  })

  it('restores the current session using the participant session token header', async () => {
    const current = makeSessionResponse('BROWSING')
    httpClientMock.get.mockResolvedValue({ data: current })

    const result = await getCurrentParticipantSession('session-token-abc')

    expect(httpClientMock.get).toHaveBeenCalledWith('/api/participant-session', {
      headers: { [PARTICIPANT_SESSION_TOKEN_HEADER]: 'session-token-abc' },
    })
    expect(result).toEqual(current)
  })

  it('reads the stable code and status out of the M5 error envelope', () => {
    const error = axiosError(409, 'PARTICIPANT_SESSION_STATE_INVALID')

    expect(getParticipantErrorCode(error)).toBe('PARTICIPANT_SESSION_STATE_INVALID')
    expect(getParticipantErrorStatus(error)).toBe(409)
  })

  it('returns null for errors that are not the M5 envelope', () => {
    expect(getParticipantErrorCode(new Error('boom'))).toBeNull()
    expect(getParticipantErrorStatus(new Error('boom'))).toBeNull()
  })

  it('PUTs the consent decision with the session token header', async () => {
    const updated = makeSessionResponse('BROWSING')
    httpClientMock.put.mockResolvedValue({ data: updated })

    const result = await decideParticipantConsent('session-token', true)

    expect(httpClientMock.put).toHaveBeenCalledWith(
      '/api/participant-session/consent',
      { accepted: true },
      { headers: { [PARTICIPANT_SESSION_TOKEN_HEADER]: 'session-token' } },
    )
    expect(result).toEqual(updated)
  })

  it('POSTs browsing completion with no body', async () => {
    const updated = makeSessionResponse('QUESTIONNAIRE')
    httpClientMock.post.mockResolvedValue({ data: updated })

    await completeParticipantBrowsing('session-token')

    expect(httpClientMock.post).toHaveBeenCalledWith(
      '/api/participant-session/browsing-completion',
      undefined,
      { headers: { [PARTICIPANT_SESSION_TOKEN_HEADER]: 'session-token' } },
    )
  })

  it('gets the current question with the session token header', async () => {
    const state = { currentQuestion: makeQuestion(), readyToSubmit: false }
    httpClientMock.get.mockResolvedValue({ data: state })

    const result = await getCurrentParticipantQuestion('session-token')

    expect(httpClientMock.get).toHaveBeenCalledWith(
      '/api/participant-session/questionnaire/current',
      { headers: { [PARTICIPANT_SESSION_TOKEN_HEADER]: 'session-token' } },
    )
    expect(result).toEqual(state)
  })

  it('PUTs an answer with both the session token and idempotency key headers', async () => {
    const state = { currentQuestion: null, readyToSubmit: true }
    httpClientMock.put.mockResolvedValue({ data: state })
    const answer = { optionId: 'opt-1', optionIds: null, scaleValue: null, textValue: null, unanswered: false }

    await submitParticipantAnswer('session-token', 'item-1', 'idem-key-1', answer)

    expect(httpClientMock.put).toHaveBeenCalledWith(
      '/api/participant-session/questionnaire/answers/item-1',
      answer,
      {
        headers: {
          [PARTICIPANT_SESSION_TOKEN_HEADER]: 'session-token',
          'Idempotency-Key': 'idem-key-1',
        },
      },
    )
  })

  it('POSTs the final questionnaire submission with no body', async () => {
    const updated = makeSessionResponse('FINISHED', { status: 'COMPLETED' })
    httpClientMock.post.mockResolvedValue({ data: updated })

    const result = await submitParticipantQuestionnaire('session-token')

    expect(httpClientMock.post).toHaveBeenCalledWith(
      '/api/participant-session/questionnaire/submission',
      undefined,
      { headers: { [PARTICIPANT_SESSION_TOKEN_HEADER]: 'session-token' } },
    )
    expect(result).toEqual(updated)
  })

  it('POSTs abandonment with no body', async () => {
    const updated = makeSessionResponse('FINISHED', {
      status: 'ABANDONED',
      abandonmentReason: 'PARTICIPANT_EXIT',
    })
    httpClientMock.post.mockResolvedValue({ data: updated })

    const result = await abandonParticipantSession('session-token')

    expect(httpClientMock.post).toHaveBeenCalledWith(
      '/api/participant-session/abandonment',
      undefined,
      { headers: { [PARTICIPANT_SESSION_TOKEN_HEADER]: 'session-token' } },
    )
    expect(result).toEqual(updated)
  })
})
