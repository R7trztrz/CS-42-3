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

function httpError(status?: number) {
  return Object.assign(new Error(`status ${status ?? 'network'}`), {
    isAxiosError: true,
    response: status === undefined ? undefined : { status, data: {} },
  })
}

const noWait = () => Promise.resolve()
const policy = { attempts: 3, baseDelayMs: 100, requestTimeoutMs: 5000 }

describe('eye-tracking delivery', () => {
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
    }
    expect(client.put).toHaveBeenCalledWith(`${CALIBRATION_ATTEMPTS_PATH}/a%2Fb`, attempt, config)
    expect(client.post).toHaveBeenCalledWith(GAZE_BATCHES_PATH, batch, config)
  })
})
