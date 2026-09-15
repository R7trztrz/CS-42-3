import type { CalibrationAttempt } from '../types/calibration'

/**
 * FR-48/FR-51 must bind authentication and durably enqueue by (sessionId, attemptId).
 * Resolve only after acceptance into that queue. Repeated calls must be idempotent.
 */
export interface CalibrationRecordSink {
  record(attempt: CalibrationAttempt): void | Promise<void>
}

/** In-memory development sink. It does not provide refresh recovery or server persistence. */
export function collectCalibrationAttempts(sessionId: string) {
  const attempts = new Map<string, CalibrationAttempt>()
  return {
    record(attempt: CalibrationAttempt): void {
      if (attempt.sessionId !== sessionId) throw new Error('Calibration session mismatch.')
      const existing = attempts.get(attempt.attemptId)
      if (
        existing &&
        existing !== attempt &&
        JSON.stringify(existing) !== JSON.stringify(attempt)
      ) {
        throw new Error('Calibration attempt ID cannot be reused with different data.')
      }
      attempts.set(attempt.attemptId, attempt)
    },
    get attempts(): readonly CalibrationAttempt[] {
      return Object.freeze([...attempts.values()])
    },
    get current(): CalibrationAttempt | null {
      return [...attempts.values()].at(-1) ?? null
    },
  }
}
