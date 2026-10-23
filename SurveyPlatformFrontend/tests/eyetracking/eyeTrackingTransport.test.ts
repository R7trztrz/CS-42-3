import { describe, expect, it, vi } from 'vitest'
import type { CalibrationAttempt, GazeEventBatch } from '../../src/eyetracking'
import {
  CALIBRATION_ATTEMPTS_PATH,
  createHttpEyeTrackingTransport,
  deliverWithRetries,
  GAZE_BATCHES_PATH,
  isRetryableDeliveryError,
} from '../../src/eyetracking/participant'
import { attemptFixture } from './gazeHelpers'
import { subscribeToStudyClosure } from '../../src/participant/model/studyClosure'

function httpError(status?: number) {
  return Object.assign(new Error(`status ${status ?? 'network'}`), {
    isAxiosError: true,
    response: status === undefined ? undefined : { status, data: {} },
  })
}

const noWait = () => Promise.resolve()
const policy = { attempts: 3, baseDelayMs: 100, requestTimeoutMs: 5000 }

describe('eye-tracking delivery', () => {
  it('notifies once on closure, aborts pending delivery, and never sends queued batches again', async () => {
    const failure = httpError(410)
    const onClosed = vi.fn()
    const unsubscribe = subscribeToStudyClosure('closed-session', onClosed)
    const client = { put: vi.fn(), post: vi.fn().mockRejectedValue(failure) }
    const transport = createHttpEyeTrackingTransport('closed-session', policy, client)
    const batch = { sessionId: 'session-A', sequence: 1 } as unknown as GazeEventBatch
    await expect(transport.sendGazeBatch(batch)).rejects.toBe(failure)
    expect(onClosed).toHaveBeenCalledTimes(1)
    expect(client.post.mock.calls[0][2].signal.aborted).toBe(true)
    await expect(transport.sendGazeBatch({ ...batch, sequence: 2 })).rejects.toBe(failure)
    await expect(transport.recordCalibrationAttempt(attemptFixture())).rejects.toBe(failure)
    expect(client.post).toHaveBeenCalledTimes(1)
    expect(client.put).not.toHaveBeenCalled()
    unsubscribe()
  })

  it('does not retry cancellation or STUDY_CLOSED even with a non-410 status', () => {
    expect(isRetryableDeliveryError({ isAxiosError: true, response: { status: 500, data: { code: 'STUDY_CLOSED' } } })).toBe(false)
    expect(isRetryableDeliveryError({ isAxiosError: true, __CANCEL__: true })).toBe(false)
  })
  it('retries network failures, timeouts, 408, 429 and 5xx but not other 4xx', () => {
    expect(isRetryableDeliveryError(httpError())).toBe(true)
    expect(isRetryableDeliveryError(httpError(408))).toBe(true)
    expect(isRetryableDeliveryError(httpError(429))).toBe(true)
    expect(isRetryableDeliveryError(httpError(503))).toBe(true)
    expect(isRetryableDeliveryError(httpError(400))).toBe(false)
    expect(isRetryableDeliveryError(httpError(409))).toBe(false)
    expect(isRetryableDeliveryError(new Error('not http'))).toBe(false)
  })

  it('backs off exponentially and gives up after the last attempt', async () => {
    const send = vi.fn().mockRejectedValue(httpError(503))
    const sleep = vi.fn(noWait)

    await expect(deliverWithRetries(send, policy, sleep)).rejects.toThrow('status 503')

    expect(send).toHaveBeenCalledTimes(3)
    expect(sleep.mock.calls).toEqual([[100], [200]])
  })

  it('stops at the first final answer and resolves once the server accepts', async () => {
    const rejected = vi.fn().mockRejectedValue(httpError(409))
    await expect(deliverWithRetries(rejected, policy, noWait)).rejects.toThrow('status 409')
    expect(rejected).toHaveBeenCalledTimes(1)

    const flaky = vi.fn().mockRejectedValueOnce(httpError()).mockResolvedValue({ status: 201 })
    await expect(deliverWithRetries(flaky, policy, noWait)).resolves.toBeUndefined()
    expect(flaky).toHaveBeenCalledTimes(2)
  })

  it('PUTs attempts by id and POSTs batches with the participant token and a timeout', async () => {
    const client = { put: vi.fn().mockResolvedValue({}), post: vi.fn().mockResolvedValue({}) }
    const transport = createHttpEyeTrackingTransport('token-123', policy, client)
    const attempt: CalibrationAttempt = attemptFixture({ attemptId: 'a/b' })
    const batch = { sessionId: 'session-A', sequence: 1 } as unknown as GazeEventBatch

    await transport.recordCalibrationAttempt(attempt)
    await transport.sendGazeBatch(batch)

    const config = {
      headers: { 'X-Participant-Session-Token': 'token-123' },
      timeout: 5000,
      signal: expect.any(AbortSignal),
    }
    expect(client.put).toHaveBeenCalledWith(`${CALIBRATION_ATTEMPTS_PATH}/a%2Fb`, attempt, config)
    expect(client.post).toHaveBeenCalledWith(GAZE_BATCHES_PATH, batch, config)
  })
})
