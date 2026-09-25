import type { EyeTrackingUnavailableReason } from '../types/calibration'

export interface GazeSample {
  readonly x: number
  readonly y: number
  /** Timestamp of this prediction on the controller's performance.now() clock. */
  readonly timestamp: number
}

/** One engine belongs to one participant session. stop() is terminal and idempotent. */
export interface GazeEngine {
  probe(): Promise<EyeTrackingUnavailableReason | null>
  start(onSample: (sample: GazeSample) => void, signal: AbortSignal): Promise<void>
  /** Changes the only prediction consumer without resetting the trained model. */
  setSampleListener(onSample: ((sample: GazeSample) => void) | null): void
  recordScreenPosition(x: number, y: number): void
  clearTrainingData(): void | Promise<void>
  /** Must release active media and clean up a late start even after cancellation. */
  stop(): void
}

export class GazeEngineUnavailableError extends Error {
  readonly reason: EyeTrackingUnavailableReason

  constructor(reason: EyeTrackingUnavailableReason, options?: ErrorOptions) {
    super(`Eye tracking unavailable: ${reason}`, options)
    this.name = 'GazeEngineUnavailableError'
    this.reason = reason
  }
}
