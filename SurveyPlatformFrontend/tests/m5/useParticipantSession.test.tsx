// @vitest-environment happy-dom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

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

import { useParticipantSession } from '../../src/participant/hooks/useParticipantSession'
import { readStoredSessionToken, storeSessionToken } from '../../src/participant/model/participantSession'
import { axiosError, makeCreateSessionResponse, makeSessionResponse } from './fixtures'

describe('useParticipantSession', () => {
  const studyToken = 'study-token-abc'

  beforeEach(() => {
    vi.clearAllMocks()
    window.sessionStorage.clear()
  })

  afterEach(() => {
    window.sessionStorage.clear()
  })

  it('starts at landing when there is no stored token', async () => {
    const { result } = renderHook(() => useParticipantSession(studyToken))

    await waitFor(() => expect(result.current.stage).toBe('landing'))
    expect(httpClientMock.get).not.toHaveBeenCalled()
  })

  it('restores an existing session from sessionStorage on mount', async () => {
    storeSessionToken(studyToken, 'stored-token')
    httpClientMock.get.mockResolvedValue({ data: makeSessionResponse('BROWSING') })

    const { result } = renderHook(() => useParticipantSession(studyToken))

    await waitFor(() => expect(result.current.stage).toBe('active'))
    expect(result.current.session?.phase).toBe('BROWSING')
    expect(result.current.sessionToken).toBe('stored-token')
  })

  it('clears a stale token and falls back to landing on 404', async () => {
    storeSessionToken(studyToken, 'stale-token')
    httpClientMock.get.mockRejectedValue(axiosError(404, 'PARTICIPANT_SESSION_NOT_FOUND'))

    const { result } = renderHook(() => useParticipantSession(studyToken))

    await waitFor(() => expect(result.current.stage).toBe('landing'))
    expect(readStoredSessionToken(studyToken)).toBeNull()
  })

  it('clears a stale token and falls back to landing on 401, without signalling a researcher-login redirect', async () => {
    storeSessionToken(studyToken, 'stale-token')
    httpClientMock.get.mockRejectedValue(axiosError(401, 'PARTICIPANT_SESSION_UNAUTHORIZED'))

    const { result } = renderHook(() => useParticipantSession(studyToken))

    await waitFor(() => expect(result.current.stage).toBe('landing'))
    expect(readStoredSessionToken(studyToken)).toBeNull()
  })

  it('moves to the closed terminal stage on 410 STUDY_CLOSED and keeps the token', async () => {
    storeSessionToken(studyToken, 'closed-token')
    httpClientMock.get.mockRejectedValue(axiosError(410, 'STUDY_CLOSED'))

    const { result } = renderHook(() => useParticipantSession(studyToken))

    await waitFor(() => expect(result.current.stage).toBe('closed'))
    expect(readStoredSessionToken(studyToken)).toBe('closed-token')
  })

  it('creates a session, persists the token, and settles on the restored shape', async () => {
    httpClientMock.post.mockResolvedValue({ data: makeCreateSessionResponse({ sessionToken: 'new-token' }) })
    httpClientMock.get.mockResolvedValue({ data: makeSessionResponse('CONSENT') })

    const { result } = renderHook(() => useParticipantSession(studyToken))
    await waitFor(() => expect(result.current.stage).toBe('landing'))

    await act(async () => {
      await result.current.createSession({ browser: 'vitest' })
    })

    expect(httpClientMock.post).toHaveBeenCalledWith(
      `/api/participation/${studyToken}/sessions`,
      { deviceInfo: { browser: 'vitest' } },
    )
    expect(readStoredSessionToken(studyToken)).toBe('new-token')
    expect(result.current.stage).toBe('active')
    expect(result.current.session?.phase).toBe('CONSENT')
  })

  it('surfaces a recoverable error on session creation failure', async () => {
    httpClientMock.post.mockRejectedValue(axiosError(500, null))

    const { result } = renderHook(() => useParticipantSession(studyToken))
    await waitFor(() => expect(result.current.stage).toBe('landing'))

    await act(async () => {
      await result.current.createSession()
    })

    expect(result.current.stage).toBe('error')
    expect(result.current.errorMessage).toBeTruthy()
    expect(readStoredSessionToken(studyToken)).toBeNull()
  })

  it('moves straight to closed when the study is closed at creation time', async () => {
    httpClientMock.post.mockRejectedValue(axiosError(410, 'STUDY_CLOSED'))

    const { result } = renderHook(() => useParticipantSession(studyToken))
    await waitFor(() => expect(result.current.stage).toBe('landing'))

    await act(async () => {
      await result.current.createSession()
    })

    expect(result.current.stage).toBe('closed')
  })

  it('retry() re-restores from a stored token', async () => {
    storeSessionToken(studyToken, 'retry-token')
    httpClientMock.get
      .mockRejectedValueOnce(axiosError(500, null))
      .mockResolvedValueOnce({ data: makeSessionResponse('BROWSING') })

    const { result } = renderHook(() => useParticipantSession(studyToken))
    await waitFor(() => expect(result.current.stage).toBe('error'))

    await act(async () => {
      await result.current.retry()
    })

    await waitFor(() => expect(result.current.stage).toBe('active'))
    expect(httpClientMock.get).toHaveBeenCalledTimes(2)
  })

  it('decideConsent updates the session from the PUT response without resetting bootstrap stage', async () => {
    storeSessionToken(studyToken, 'token')
    httpClientMock.get.mockResolvedValue({ data: makeSessionResponse('CONSENT') })
    httpClientMock.put.mockResolvedValue({ data: makeSessionResponse('BROWSING', { consentedAt: '2026-10-05T00:00:00Z' }) })

    const { result } = renderHook(() => useParticipantSession(studyToken))
    await waitFor(() => expect(result.current.session?.phase).toBe('CONSENT'))

    let success = false
    await act(async () => {
      success = await result.current.decideConsent(true)
    })

    expect(success).toBe(true)
    expect(result.current.stage).toBe('active')
    expect(result.current.session?.phase).toBe('BROWSING')
    expect(httpClientMock.put).toHaveBeenCalledWith(
      '/api/participant-session/consent',
      { accepted: true },
      expect.anything(),
    )
  })

  it('decideConsent(false) reflects a decline into ABANDONED/CONSENT_DECLINED', async () => {
    storeSessionToken(studyToken, 'token')
    httpClientMock.get.mockResolvedValue({ data: makeSessionResponse('CONSENT') })
    httpClientMock.put.mockResolvedValue({
      data: makeSessionResponse('FINISHED', { status: 'ABANDONED', abandonmentReason: 'CONSENT_DECLINED' }),
    })

    const { result } = renderHook(() => useParticipantSession(studyToken))
    await waitFor(() => expect(result.current.session?.phase).toBe('CONSENT'))

    await act(async () => {
      await result.current.decideConsent(false)
    })

    expect(result.current.session?.status).toBe('ABANDONED')
    expect(result.current.session?.abandonmentReason).toBe('CONSENT_DECLINED')
  })

  it('completeBrowsing advances the session using the browsing-completion response', async () => {
    storeSessionToken(studyToken, 'token')
    httpClientMock.get.mockResolvedValue({ data: makeSessionResponse('BROWSING') })
    httpClientMock.post.mockResolvedValue({ data: makeSessionResponse('QUESTIONNAIRE') })

    const { result } = renderHook(() => useParticipantSession(studyToken))
    await waitFor(() => expect(result.current.session?.phase).toBe('BROWSING'))

    await act(async () => {
      await result.current.completeBrowsing()
    })

    expect(result.current.session?.phase).toBe('QUESTIONNAIRE')
  })

  it('abandon() moves the session to ABANDONED/PARTICIPANT_EXIT', async () => {
    storeSessionToken(studyToken, 'token')
    httpClientMock.get.mockResolvedValue({ data: makeSessionResponse('BROWSING') })
    httpClientMock.post.mockResolvedValue({
      data: makeSessionResponse('FINISHED', { status: 'ABANDONED', abandonmentReason: 'PARTICIPANT_EXIT' }),
    })

    const { result } = renderHook(() => useParticipantSession(studyToken))
    await waitFor(() => expect(result.current.session?.phase).toBe('BROWSING'))

    await act(async () => {
      await result.current.abandon()
    })

    expect(result.current.session?.status).toBe('ABANDONED')
    expect(result.current.session?.abandonmentReason).toBe('PARTICIPANT_EXIT')
  })

  it('a session action that 401s drops the token and returns to landing instead of failing silently', async () => {
    storeSessionToken(studyToken, 'token')
    httpClientMock.get.mockResolvedValue({ data: makeSessionResponse('BROWSING') })
    httpClientMock.post.mockRejectedValue(axiosError(401, 'PARTICIPANT_SESSION_UNAUTHORIZED'))

    const { result } = renderHook(() => useParticipantSession(studyToken))
    await waitFor(() => expect(result.current.session?.phase).toBe('BROWSING'))

    await act(async () => {
      await result.current.completeBrowsing()
    })

    expect(result.current.stage).toBe('landing')
    expect(readStoredSessionToken(studyToken)).toBeNull()
  })

  it('a session action that 410s moves to the closed terminal stage', async () => {
    storeSessionToken(studyToken, 'token')
    httpClientMock.get.mockResolvedValue({ data: makeSessionResponse('BROWSING') })
    httpClientMock.post.mockRejectedValue(axiosError(410, 'STUDY_CLOSED'))

    const { result } = renderHook(() => useParticipantSession(studyToken))
    await waitFor(() => expect(result.current.session?.phase).toBe('BROWSING'))

    await act(async () => {
      await result.current.completeBrowsing()
    })

    expect(result.current.stage).toBe('closed')
  })

  it('a session action that fails for an ordinary reason surfaces actionError without losing the session', async () => {
    storeSessionToken(studyToken, 'token')
    httpClientMock.get.mockResolvedValue({ data: makeSessionResponse('BROWSING') })
    httpClientMock.post.mockRejectedValue(axiosError(409, 'PARTICIPANT_SESSION_STATE_INVALID'))

    const { result } = renderHook(() => useParticipantSession(studyToken))
    await waitFor(() => expect(result.current.session?.phase).toBe('BROWSING'))

    await act(async () => {
      await result.current.completeBrowsing()
    })

    expect(result.current.stage).toBe('active')
    expect(result.current.session?.phase).toBe('BROWSING')
    expect(result.current.actionErrorCode).toBe('PARTICIPANT_SESSION_STATE_INVALID')
  })
})
