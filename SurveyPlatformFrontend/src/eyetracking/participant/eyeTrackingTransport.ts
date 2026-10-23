import axios, { type AxiosInstance } from 'axios'
import participantHttpClient from '../../participant/api/participantHttpClient'
import { PARTICIPANT_SESSION_TOKEN_HEADER } from '../../participant/model/participantSession'
import { isStudyClosedError, notifyStudyClosed } from '../../participant/model/studyClosure'
import type { GazeEventBatch } from '../gaze/types'
import type { CalibrationAttempt } from '../types/calibration'

/** Delivery to the M6 backend. Each call resolves only once the server has stored the record. */
export interface EyeTrackingTransport {
  recordCalibrationAttempt(attempt: CalibrationAttempt): Promise<void>
  sendGazeBatch(batch: GazeEventBatch): Promise<void>
  cancelPendingDelivery?(): void
}

export interface DeliveryRetryPolicy {
  /** Total tries, including the first. */
  readonly attempts: number
  /** Doubles after every retry. */
  readonly baseDelayMs: number
  readonly requestTimeoutMs: number
}

/**
 * Bounded so a stalled network cannot hold the single in-flight gaze batch for long: the FR-50
 * buffer keeps collecting meanwhile and reports any overflow on the next batch.
 */
export const DEFAULT_DELIVERY_RETRY_POLICY: DeliveryRetryPolicy = Object.freeze({
  attempts: 3,
  baseDelayMs: 500,
  requestTimeoutMs: 10_000,
})

/**
 * Network failures, timeouts, 408, 429 and 5xx are worth repeating: every endpoint is idempotent,
 * so a retry of a request that did reach the server is acknowledged rather than duplicated.
 * Other 4xx answers are final.
 */
export function isRetryableDeliveryError(error: unknown): boolean {
  if (isStudyClosedError(error) || axios.isCancel(error)) return false
  if (!axios.isAxiosError(error)) return false
  const status = error.response?.status
  if (status === undefined) return true
  return status === 408 || status === 429 || status >= 500
}

export async function deliverWithRetries(
  send: () => Promise<unknown>,
  policy: DeliveryRetryPolicy = DEFAULT_DELIVERY_RETRY_POLICY,
  sleep: (ms: number) => Promise<void> = (ms) => new Promise((resolve) => setTimeout(resolve, ms)),
): Promise<void> {
  for (let attempt = 1; ; attempt += 1) {
    try {
      await send()
      return
    } catch (error) {
      if (attempt >= policy.attempts || !isRetryableDeliveryError(error)) throw error
      await sleep(policy.baseDelayMs * 2 ** (attempt - 1))
    }
  }
}

export const CALIBRATION_ATTEMPTS_PATH = '/api/participant-session/calibration-attempts'
export const GAZE_BATCHES_PATH = '/api/participant-session/gaze-batches'

export function createHttpEyeTrackingTransport(
  sessionToken: string,
  policy: DeliveryRetryPolicy = DEFAULT_DELIVERY_RETRY_POLICY,
  client: Pick<AxiosInstance, 'put' | 'post'> = participantHttpClient,
): EyeTrackingTransport {
  const abort = new AbortController()
  let terminalError: unknown = null
  let stopped = false
  const config = {
    headers: { [PARTICIPANT_SESSION_TOKEN_HEADER]: sessionToken },
    timeout: policy.requestTimeoutMs,
    signal: abort.signal,
  }

  async function deliver(send: () => Promise<unknown>): Promise<void> {
    try {
      await deliverWithRetries(() => {
        if (stopped) return Promise.reject(terminalError)
        return send()
      }, policy)
    } catch (error) {
      if (isStudyClosedError(error) && !stopped) {
        stopped = true
        terminalError = error
        abort.abort()
        notifyStudyClosed(sessionToken)
      }
      throw error
    }
  }

  return {
    cancelPendingDelivery() {
      stopped = true
      terminalError ??= new Error('Eye-tracking delivery has ended.')
      abort.abort()
    },
    recordCalibrationAttempt: (attempt) =>
      deliver(
        () =>
          client.put(
            `${CALIBRATION_ATTEMPTS_PATH}/${encodeURIComponent(attempt.attemptId)}`,
            attempt,
            config,
          ),
      ),
    sendGazeBatch: (batch) =>
      deliver(() => client.post(GAZE_BATCHES_PATH, batch, config)),
  }
}
