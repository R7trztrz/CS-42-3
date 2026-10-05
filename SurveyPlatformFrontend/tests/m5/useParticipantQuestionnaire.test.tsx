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
import { axiosError, makeQuestion } from './fixtures'

const EMPTY_ANSWER: SubmitAnswerPayload = {
  optionId: null,
  optionIds: null,
  scaleValue: null,
  textValue: null,
  unanswered: false,
}

describe('useParticipantQuestionnaire', () => {
  beforeEach(() => {
    vi.clearAllMocks()
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
})
