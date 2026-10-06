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
import { axiosError, deferred, makeCreateSessionResponse, makeSessionResponse } from './fixtures'

describe('useParticipantSession', () => {
  const studyToken = 'study-token-abc'

  beforeEach(() => {
    vi.resetAllMocks()
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

  it('invalidates a delayed restore when switching to a study with no stored session', async () => {
    const oldRestore = deferred<{ data: ReturnType<typeof makeSessionResponse> }>()
    storeSessionToken(studyToken, 'token-a')
    httpClientMock.get.mockReturnValue(oldRestore.promise)
    const { result, rerender } = renderHook(({ token }) => useParticipantSession(token), {
      initialProps: { token: studyToken },
    })
    await waitFor(() => expect(httpClientMock.get).toHaveBeenCalledTimes(1))
    rerender({ token: 'study-b' })
    await waitFor(() => expect(result.current.stage).toBe('landing'))
    await act(async () => { oldRestore.resolve({ data: makeSessionResponse('BROWSING') }) })
    expect(result.current.stage).toBe('landing')
    expect(result.current.session).toBeNull()
    expect(result.current.sessionToken).toBeNull()
  })

  it('clears old session state immediately when switching to a study with no token', async () => {
    storeSessionToken(studyToken, 'token-a')
    httpClientMock.get.mockResolvedValue({ data: makeSessionResponse('BROWSING') })
    const { result, rerender } = renderHook(({ token }) => useParticipantSession(token), {
      initialProps: { token: studyToken },
    })
    await waitFor(() => expect(result.current.stage).toBe('active'))
    rerender({ token: 'study-b' })
    await waitFor(() => expect(result.current.stage).toBe('landing'))
    expect(result.current.sessionToken).toBeNull()
    expect(result.current.session).toBeNull()
    expect(await result.current.completeBrowsing()).toBe(false)
    expect(httpClientMock.post).not.toHaveBeenCalled()
  })

  it.each(['switch', 'unmount'])('does not persist a delayed creation after %s', async (transition) => {
    const creation = deferred<{ data: ReturnType<typeof makeCreateSessionResponse> }>()
    httpClientMock.post.mockReturnValue(creation.promise)
    const { result, rerender, unmount } = renderHook(({ token }) => useParticipantSession(token), {
      initialProps: { token: studyToken },
    })
    await waitFor(() => expect(result.current.stage).toBe('landing'))
    let pending!: Promise<void>
    act(() => { pending = result.current.createSession() })
    if (transition === 'switch') rerender({ token: 'study-b' })
    else unmount()
    await act(async () => {
      creation.resolve({ data: makeCreateSessionResponse({ sessionToken: 'late-token-a' }) })
      await pending
    })
    expect(readStoredSessionToken(studyToken)).toBeNull()
    expect(httpClientMock.get).not.toHaveBeenCalled()
    if (transition === 'switch') expect(result.current.stage).toBe('landing')
  })

  it.each(['success', 'unauthorized', 'closed'])('ignores a late %s action after another study becomes active', async (outcome) => {
    const action = deferred<{ data: ReturnType<typeof makeSessionResponse> }>()
    storeSessionToken(studyToken, 'token-a')
    storeSessionToken('study-b', 'token-b')
    httpClientMock.get
      .mockResolvedValueOnce({ data: makeSessionResponse('BROWSING', { sessionId: 'session-a' }) })
      .mockResolvedValueOnce({ data: makeSessionResponse('CONSENT', { sessionId: 'session-b' }) })
    httpClientMock.post.mockReturnValue(action.promise)
    const { result, rerender } = renderHook(({ token }) => useParticipantSession(token), {
      initialProps: { token: studyToken },
    })
    await waitFor(() => expect(result.current.session?.sessionId).toBe('session-a'))
    let pending!: Promise<boolean>
    act(() => { pending = result.current.completeBrowsing() })
    rerender({ token: 'study-b' })
    await waitFor(() => expect(result.current.session?.sessionId).toBe('session-b'))
    let succeeded = true
    await act(async () => {
      if (outcome === 'success') action.resolve({ data: makeSessionResponse('QUESTIONNAIRE', { sessionId: 'session-a' }) })
      else action.reject(axiosError(outcome === 'closed' ? 410 : 401, outcome === 'closed' ? 'STUDY_CLOSED' : 'PARTICIPANT_SESSION_UNAUTHORIZED'))
      succeeded = await pending
    })
    expect(succeeded).toBe(false)
    expect(result.current.stage).toBe('active')
    expect(result.current.session?.sessionId).toBe('session-b')
    expect(result.current.sessionToken).toBe('token-b')
    expect(readStoredSessionToken(studyToken)).toBe('token-a')
    expect(readStoredSessionToken('study-b')).toBe('token-b')
    expect(result.current.actionErrorCode).toBeNull()
  })

  it('does not let an old action clear the pending flag of a new-study action', async () => {
    const oldAction = deferred<{ data: ReturnType<typeof makeSessionResponse> }>()
    const newAction = deferred<{ data: ReturnType<typeof makeSessionResponse> }>()
    storeSessionToken(studyToken, 'token-a')
    storeSessionToken('study-b', 'token-b')
    httpClientMock.get.mockResolvedValue({ data: makeSessionResponse('BROWSING') })
    httpClientMock.post.mockReturnValueOnce(oldAction.promise).mockReturnValueOnce(newAction.promise)
    const { result, rerender } = renderHook(({ token }) => useParticipantSession(token), {
      initialProps: { token: studyToken },
    })
    await waitFor(() => expect(result.current.stage).toBe('active'))
    let oldPending!: Promise<boolean>
    let newPending!: Promise<boolean>
    act(() => { oldPending = result.current.completeBrowsing() })
    rerender({ token: 'study-b' })
    await waitFor(() => expect(result.current.sessionToken).toBe('token-b'))
    act(() => { newPending = result.current.completeBrowsing() })
    await act(async () => { oldAction.resolve({ data: makeSessionResponse('QUESTIONNAIRE') }); await oldPending })
    expect(result.current.isActionPending).toBe(true)
    await act(async () => { newAction.resolve({ data: makeSessionResponse('QUESTIONNAIRE') }); await newPending })
    expect(result.current.isActionPending).toBe(false)
  })

  it('rejects a stale callback even after switching A to B and back to A', async () => {
    storeSessionToken(studyToken, 'token-a')
    httpClientMock.get.mockResolvedValue({ data: makeSessionResponse('BROWSING') })
    httpClientMock.post.mockResolvedValue({ data: makeSessionResponse('QUESTIONNAIRE') })
    const { result, rerender } = renderHook(({ token }) => useParticipantSession(token), {
      initialProps: { token: studyToken },
    })
    await waitFor(() => expect(result.current.stage).toBe('active'))
    const oldComplete = result.current.completeBrowsing
    rerender({ token: 'study-b' })
    rerender({ token: studyToken })
    await waitFor(() => expect(result.current.stage).toBe('active'))
    expect(await oldComplete()).toBe(false)
    expect(httpClientMock.post).not.toHaveBeenCalled()
  })

  it('prevents concurrent duplicate creation calls', async () => {
    const creation = deferred<{ data: ReturnType<typeof makeCreateSessionResponse> }>()
    httpClientMock.post.mockReturnValue(creation.promise)
    httpClientMock.get.mockResolvedValue({ data: makeSessionResponse('CONSENT') })
    const { result } = renderHook(() => useParticipantSession(studyToken))
    await waitFor(() => expect(result.current.stage).toBe('landing'))
    await act(async () => {
      const first = result.current.createSession()
      const second = result.current.createSession()
      creation.resolve({ data: makeCreateSessionResponse() })
      await Promise.all([first, second])
    })
    expect(httpClientMock.post).toHaveBeenCalledTimes(1)
  })
})
