// @vitest-environment happy-dom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

const { httpClientMock } = vi.hoisted(() => ({
  httpClientMock: {
    get: vi.fn(),
    put: vi.fn(),
  },
}))

vi.mock('../../src/participant/api/participantHttpClient', () => ({
  default: httpClientMock,
}))

import { useParticipantQuestionnaire } from '../../src/participant/hooks/useParticipantQuestionnaire'
import type { SubmitAnswerPayload } from '../../src/participant/model/participantSession'
import { axiosError, deferred, makeQuestion } from './fixtures'

const EMPTY_ANSWER: SubmitAnswerPayload = {
  optionId: null,
  optionIds: null,
  scaleValue: null,
  textValue: null,
  unanswered: false,
}

describe('useParticipantQuestionnaire', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('loads the current question on mount', async () => {
    httpClientMock.get.mockResolvedValue({
      data: { currentQuestion: makeQuestion(), readyToSubmit: false },
    })

    const { result } = renderHook(() => useParticipantQuestionnaire('session-token'))

    await waitFor(() => expect(result.current.currentQuestion).not.toBeNull())
    expect(result.current.currentQuestion?.itemId).toBe('item-1')
    expect(result.current.readyToSubmit).toBe(false)
  })

  it('shows readyToSubmit once the branch path reaches END', async () => {
    httpClientMock.get.mockResolvedValue({
      data: { currentQuestion: null, readyToSubmit: true },
    })

    const { result } = renderHook(() => useParticipantQuestionnaire('session-token'))

    await waitFor(() => expect(result.current.readyToSubmit).toBe(true))
    expect(result.current.currentQuestion).toBeNull()
  })

  it('submits an answer and advances to the next question from the server response', async () => {
    const first = makeQuestion({ itemId: 'item-1' })
    const second = makeQuestion({ itemId: 'item-2', position: 1 })
    httpClientMock.get.mockResolvedValue({ data: { currentQuestion: first, readyToSubmit: false } })
    httpClientMock.put.mockResolvedValue({ data: { currentQuestion: second, readyToSubmit: false } })

    const { result } = renderHook(() => useParticipantQuestionnaire('session-token'))
    await waitFor(() => expect(result.current.currentQuestion?.itemId).toBe('item-1'))

    await act(async () => {
      await result.current.submitAnswer('item-1', { ...EMPTY_ANSWER, optionId: 'opt-yes' })
    })

    expect(result.current.currentQuestion?.itemId).toBe('item-2')
    expect(httpClientMock.put).toHaveBeenCalledTimes(1)
  })

  it('reuses the same idempotency key for an identical retry of the same answer', async () => {
    const question = makeQuestion({ itemId: 'item-1' })
    httpClientMock.get.mockResolvedValue({ data: { currentQuestion: question, readyToSubmit: false } })
    httpClientMock.put.mockResolvedValue({ data: { currentQuestion: question, readyToSubmit: false } })

    const { result } = renderHook(() => useParticipantQuestionnaire('session-token'))
    await waitFor(() => expect(result.current.currentQuestion).not.toBeNull())

    const answer = { ...EMPTY_ANSWER, optionId: 'opt-yes' }

    await act(async () => {
      await result.current.submitAnswer('item-1', answer)
    })
    await act(async () => {
      await result.current.submitAnswer('item-1', answer)
    })

    const firstKey = httpClientMock.put.mock.calls[0][2].headers['Idempotency-Key']
    const secondKey = httpClientMock.put.mock.calls[1][2].headers['Idempotency-Key']
    expect(secondKey).toBe(firstKey)
  })

  it('mints a new idempotency key when the answer changes before resubmitting', async () => {
    const question = makeQuestion({ itemId: 'item-1' })
    httpClientMock.get.mockResolvedValue({ data: { currentQuestion: question, readyToSubmit: false } })
    httpClientMock.put.mockResolvedValue({ data: { currentQuestion: question, readyToSubmit: false } })

    const { result } = renderHook(() => useParticipantQuestionnaire('session-token'))
    await waitFor(() => expect(result.current.currentQuestion).not.toBeNull())

    await act(async () => {
      await result.current.submitAnswer('item-1', { ...EMPTY_ANSWER, optionId: 'opt-yes' })
    })
    await act(async () => {
      await result.current.submitAnswer('item-1', { ...EMPTY_ANSWER, optionId: 'opt-no' })
    })

    const firstKey = httpClientMock.put.mock.calls[0][2].headers['Idempotency-Key']
    const secondKey = httpClientMock.put.mock.calls[1][2].headers['Idempotency-Key']
    expect(secondKey).not.toBe(firstKey)
  })

  it('resyncs from the server on PARTICIPANT_QUESTION_NOT_CURRENT instead of guessing', async () => {
    const stale = makeQuestion({ itemId: 'item-1' })
    const actuallyCurrent = makeQuestion({ itemId: 'item-2', position: 1 })

    httpClientMock.get
      .mockResolvedValueOnce({ data: { currentQuestion: stale, readyToSubmit: false } })
      .mockResolvedValueOnce({ data: { currentQuestion: actuallyCurrent, readyToSubmit: false } })
    httpClientMock.put.mockRejectedValue(axiosError(409, 'PARTICIPANT_QUESTION_NOT_CURRENT'))

    const { result } = renderHook(() => useParticipantQuestionnaire('session-token'))
    await waitFor(() => expect(result.current.currentQuestion?.itemId).toBe('item-1'))

    await act(async () => {
      await result.current.submitAnswer('item-1', { ...EMPTY_ANSWER, optionId: 'opt-yes' })
    })

    expect(result.current.currentQuestion?.itemId).toBe('item-2')
    expect(result.current.errorCode).toBe('PARTICIPANT_QUESTION_NOT_CURRENT')
  })

  it('does not blindly retry on an idempotency conflict, and resyncs instead', async () => {
    const question = makeQuestion({ itemId: 'item-1' })
    httpClientMock.get.mockResolvedValue({ data: { currentQuestion: question, readyToSubmit: false } })
    httpClientMock.put.mockRejectedValue(axiosError(409, 'PARTICIPANT_IDEMPOTENCY_CONFLICT'))

    const { result } = renderHook(() => useParticipantQuestionnaire('session-token'))
    await waitFor(() => expect(result.current.currentQuestion).not.toBeNull())

    await act(async () => {
      await result.current.submitAnswer('item-1', { ...EMPTY_ANSWER, optionId: 'opt-yes' })
    })

    expect(result.current.errorCode).toBe('PARTICIPANT_IDEMPOTENCY_CONFLICT')
    // One load on mount, one reload after the conflict.
    expect(httpClientMock.get).toHaveBeenCalledTimes(2)
  })

  it('ignores an old-session load that finishes after the new-session load', async () => {
    const oldLoad = deferred<{ data: { currentQuestion: ReturnType<typeof makeQuestion>; readyToSubmit: boolean } }>()
    httpClientMock.get.mockReturnValueOnce(oldLoad.promise)
      .mockResolvedValueOnce({ data: { currentQuestion: makeQuestion({ itemId: 'item-b' }), readyToSubmit: false } })
    const { result, rerender } = renderHook(({ token }) => useParticipantQuestionnaire(token), {
      initialProps: { token: 'token-a' },
    })
    rerender({ token: 'token-b' })
    await waitFor(() => expect(result.current.currentQuestion?.itemId).toBe('item-b'))
    await act(async () => { oldLoad.resolve({ data: { currentQuestion: makeQuestion({ itemId: 'item-a' }), readyToSubmit: false } }) })
    expect(result.current.currentQuestion?.itemId).toBe('item-b')
  })

  it('clears question, completion and error state when the session disappears', async () => {
    httpClientMock.get.mockResolvedValue({ data: { currentQuestion: null, readyToSubmit: true } })
    const { result, rerender } = renderHook(({ token }: { token: string | null }) => useParticipantQuestionnaire(token), {
      initialProps: { token: 'token-a' },
    })
    await waitFor(() => expect(result.current.readyToSubmit).toBe(true))
    rerender({ token: null })
    expect(result.current.readyToSubmit).toBe(false)
    expect(result.current.currentQuestion).toBeNull()
    expect(result.current.isLoading).toBe(false)
    expect(result.current.isSubmitting).toBe(false)
  })

  it.each(['success', 'conflict'])('ignores a late answer %s and does not resync the old session', async (outcome) => {
    const answer = deferred<{ data: { currentQuestion: ReturnType<typeof makeQuestion> | null; readyToSubmit: boolean } }>()
    httpClientMock.get.mockResolvedValueOnce({ data: { currentQuestion: makeQuestion({ itemId: 'item-a' }), readyToSubmit: false } })
      .mockResolvedValueOnce({ data: { currentQuestion: makeQuestion({ itemId: 'item-b' }), readyToSubmit: false } })
    httpClientMock.put.mockReturnValue(answer.promise)
    const { result, rerender } = renderHook(({ token }) => useParticipantQuestionnaire(token), {
      initialProps: { token: 'token-a' },
    })
    await waitFor(() => expect(result.current.currentQuestion?.itemId).toBe('item-a'))
    let pending!: Promise<boolean>
    act(() => { pending = result.current.submitAnswer('item-a', { ...EMPTY_ANSWER, optionId: 'opt-yes' }) })
    rerender({ token: 'token-b' })
    await waitFor(() => expect(result.current.currentQuestion?.itemId).toBe('item-b'))
    let succeeded = true
    await act(async () => {
      if (outcome === 'success') answer.resolve({ data: { currentQuestion: null, readyToSubmit: true } })
      else answer.reject(axiosError(409, 'PARTICIPANT_QUESTION_NOT_CURRENT'))
      succeeded = await pending
    })
    expect(succeeded).toBe(false)
    expect(result.current.currentQuestion?.itemId).toBe('item-b')
    expect(result.current.readyToSubmit).toBe(false)
    expect(result.current.errorCode).toBeNull()
    expect(httpClientMock.get).toHaveBeenCalledTimes(2)
  })

  it('does not resync a failed answer after unmount', async () => {
    const answer = deferred<{ data: { currentQuestion: null; readyToSubmit: boolean } }>()
    httpClientMock.get.mockResolvedValue({ data: { currentQuestion: makeQuestion(), readyToSubmit: false } })
    httpClientMock.put.mockReturnValue(answer.promise)
    const { result, unmount } = renderHook(() => useParticipantQuestionnaire('token-a'))
    await waitFor(() => expect(result.current.currentQuestion).not.toBeNull())
    let pending!: Promise<boolean>
    act(() => { pending = result.current.submitAnswer('item-1', { ...EMPTY_ANSWER, optionId: 'opt-yes' }) })
    unmount()
    await act(async () => { answer.reject(axiosError(409, 'PARTICIPANT_QUESTION_NOT_CURRENT')); expect(await pending).toBe(false) })
    expect(httpClientMock.get).toHaveBeenCalledTimes(1)
  })

  it('only applies the latest reload response within one session', async () => {
    const oldLoad = deferred<{ data: { currentQuestion: ReturnType<typeof makeQuestion>; readyToSubmit: boolean } }>()
    httpClientMock.get.mockResolvedValueOnce({ data: { currentQuestion: makeQuestion(), readyToSubmit: false } })
      .mockReturnValueOnce(oldLoad.promise)
      .mockResolvedValueOnce({ data: { currentQuestion: makeQuestion({ itemId: 'latest' }), readyToSubmit: false } })
    const { result } = renderHook(() => useParticipantQuestionnaire('token-a'))
    await waitFor(() => expect(result.current.currentQuestion).not.toBeNull())
    let older!: Promise<void>
    act(() => { older = result.current.reload() })
    await act(async () => { await result.current.reload() })
    await act(async () => { oldLoad.resolve({ data: { currentQuestion: makeQuestion({ itemId: 'older' }), readyToSubmit: false } }); await older })
    expect(result.current.currentQuestion?.itemId).toBe('latest')
  })

  it('keeps idempotency keys isolated by session even for the same published item', async () => {
    httpClientMock.get.mockResolvedValue({ data: { currentQuestion: makeQuestion(), readyToSubmit: false } })
    httpClientMock.put.mockResolvedValue({ data: { currentQuestion: makeQuestion(), readyToSubmit: false } })
    const { result, rerender } = renderHook(({ token }) => useParticipantQuestionnaire(token), {
      initialProps: { token: 'token-a' },
    })
    await waitFor(() => expect(result.current.currentQuestion).not.toBeNull())
    await act(async () => { await result.current.submitAnswer('item-1', { ...EMPTY_ANSWER, optionId: 'opt-yes' }) })
    const firstKey = httpClientMock.put.mock.calls[0][2].headers['Idempotency-Key']
    rerender({ token: 'token-b' })
    await act(async () => { await result.current.submitAnswer('item-1', { ...EMPTY_ANSWER, optionId: 'opt-yes' }) })
    expect(httpClientMock.put.mock.calls[1][2].headers['Idempotency-Key']).not.toBe(firstKey)
  })

  it('does not let a delayed reload overwrite the result of a newer answer', async () => {
    const reload = deferred<{ data: { currentQuestion: ReturnType<typeof makeQuestion>; readyToSubmit: boolean } }>()
    httpClientMock.get.mockResolvedValueOnce({ data: { currentQuestion: makeQuestion(), readyToSubmit: false } }).mockReturnValueOnce(reload.promise)
    httpClientMock.put.mockResolvedValue({ data: { currentQuestion: makeQuestion({ itemId: 'after-answer' }), readyToSubmit: false } })
    const { result } = renderHook(() => useParticipantQuestionnaire('token-a'))
    await waitFor(() => expect(result.current.currentQuestion).not.toBeNull())
    let pendingLoad!: Promise<void>
    act(() => { pendingLoad = result.current.reload() })
    await act(async () => { await result.current.submitAnswer('item-1', { ...EMPTY_ANSWER, optionId: 'opt-yes' }) })
    await act(async () => { reload.resolve({ data: { currentQuestion: makeQuestion(), readyToSubmit: false } }); await pendingLoad })
    expect(result.current.currentQuestion?.itemId).toBe('after-answer')
    expect(result.current.isLoading).toBe(false)
  })

  it('keeps a new submission pending when an old-session submission finishes', async () => {
    const oldAnswer = deferred<{ data: { currentQuestion: null; readyToSubmit: boolean } }>()
    const newAnswer = deferred<{ data: { currentQuestion: null; readyToSubmit: boolean } }>()
    httpClientMock.get.mockResolvedValue({ data: { currentQuestion: makeQuestion(), readyToSubmit: false } })
    httpClientMock.put.mockReturnValueOnce(oldAnswer.promise).mockReturnValueOnce(newAnswer.promise)
    const { result, rerender } = renderHook(({ token }) => useParticipantQuestionnaire(token), { initialProps: { token: 'token-a' } })
    await waitFor(() => expect(result.current.currentQuestion).not.toBeNull())
    let oldPending!: Promise<boolean>
    let newPending!: Promise<boolean>
    act(() => { oldPending = result.current.submitAnswer('item-1', EMPTY_ANSWER) })
    rerender({ token: 'token-b' })
    await waitFor(() => expect(result.current.currentQuestion).not.toBeNull())
    act(() => { newPending = result.current.submitAnswer('item-1', EMPTY_ANSWER) })
    await act(async () => { oldAnswer.resolve({ data: { currentQuestion: null, readyToSubmit: true } }); await oldPending })
    expect(result.current.isSubmitting).toBe(true)
    expect(result.current.readyToSubmit).toBe(false)
    await act(async () => { newAnswer.resolve({ data: { currentQuestion: null, readyToSubmit: true } }); await newPending })
    expect(result.current.isSubmitting).toBe(false)
    expect(result.current.readyToSubmit).toBe(true)
  })

  it('prevents concurrent duplicate answer submissions', async () => {
    const answer = deferred<{ data: { currentQuestion: null; readyToSubmit: boolean } }>()
    httpClientMock.get.mockResolvedValue({ data: { currentQuestion: makeQuestion(), readyToSubmit: false } })
    httpClientMock.put.mockReturnValue(answer.promise)
    const { result } = renderHook(() => useParticipantQuestionnaire('token-a'))
    await waitFor(() => expect(result.current.currentQuestion).not.toBeNull())
    await act(async () => {
      const first = result.current.submitAnswer('item-1', EMPTY_ANSWER)
      expect(await result.current.submitAnswer('item-1', EMPTY_ANSWER)).toBe(false)
      answer.resolve({ data: { currentQuestion: null, readyToSubmit: true } })
      expect(await first).toBe(true)
    })
    expect(httpClientMock.put).toHaveBeenCalledTimes(1)
  })

  it('does not apply a conflict-resync result or error after the session changes', async () => {
    const resync = deferred<{ data: { currentQuestion: null; readyToSubmit: boolean } }>()
    httpClientMock.get.mockResolvedValueOnce({ data: { currentQuestion: makeQuestion(), readyToSubmit: false } })
      .mockReturnValueOnce(resync.promise)
      .mockResolvedValueOnce({ data: { currentQuestion: makeQuestion({ itemId: 'item-b' }), readyToSubmit: false } })
    httpClientMock.put.mockRejectedValue(axiosError(409, 'PARTICIPANT_QUESTION_NOT_CURRENT'))
    const { result, rerender } = renderHook(({ token }) => useParticipantQuestionnaire(token), { initialProps: { token: 'token-a' } })
    await waitFor(() => expect(result.current.currentQuestion).not.toBeNull())
    let pending!: Promise<boolean>
    act(() => { pending = result.current.submitAnswer('item-1', EMPTY_ANSWER) })
    await waitFor(() => expect(httpClientMock.get).toHaveBeenCalledTimes(2))
    rerender({ token: 'token-b' })
    await waitFor(() => expect(result.current.currentQuestion?.itemId).toBe('item-b'))
    await act(async () => { resync.resolve({ data: { currentQuestion: null, readyToSubmit: true } }); expect(await pending).toBe(false) })
    expect(result.current.currentQuestion?.itemId).toBe('item-b')
    expect(result.current.errorCode).toBeNull()
    expect(result.current.readyToSubmit).toBe(false)
  })
})
