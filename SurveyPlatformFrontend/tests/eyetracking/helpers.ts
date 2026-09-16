import { vi } from 'vitest'
import { CalibrationController } from '../../src/eyetracking'
import type {
  CalibrationControllerOptions,
  EyeTrackingUnavailableReason,
  GazeEngine,
  GazeSample,
} from '../../src/eyetracking'

export function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason: unknown) => void
  const promise = new Promise<T>((yes, no) => {
    resolve = yes
    reject = no
  })
  return { promise, resolve, reject }
}

export class FakeEngine implements GazeEngine {
  listener: ((sample: GazeSample) => void) | null = null
  startGate: Promise<void> = Promise.resolve()
  probe = vi.fn(async (): Promise<EyeTrackingUnavailableReason | null> => null)
  clearTrainingData = vi.fn(async () => {})
  recordScreenPosition = vi.fn<(x: number, y: number) => void>()
  stop = vi.fn(() => {
    this.listener = null
  })
  setSampleListener = vi.fn((listener: ((sample: GazeSample) => void) | null) => {
    this.listener = listener
  })
  start = vi.fn<GazeEngine['start']>(async (listener) => {
    await this.startGate
    this.listener = listener
  })
  emit(x: number, y: number, timestamp: number) {
    this.listener?.({ x, y, timestamp })
  }
}

export async function flush() {
  for (let i = 0; i < 12; i += 1) await Promise.resolve()
}

export function setup(options: Partial<CalibrationControllerOptions> = {}) {
  const engine = new FakeEngine()
  const clock = { time: 0, wall: 1_700_000_000_000 }
  const viewport = { width: 1000, height: 800, devicePixelRatio: 2 }
  let sequence = 0
  const controller = new CalibrationController({
    engine,
    session: { sessionId: 'session-A', consentGiven: true, eyeTrackingEnabled: true },
    readViewport: () => viewport,
    now: () => clock.time,
    wallClock: () => clock.wall,
    createAttemptId: () => `attempt-${++sequence}`,
    targetSettleMs: 0,
    ...options,
  })
  const click = (offset = 0) => {
    const target = controller.currentTarget!
    clock.time += 201
    engine.emit(target.targetX, target.targetY + offset, clock.time)
    controller.registerClick()
  }
  const complete = (offset = 0) => {
    while (controller.running) click(offset)
  }
  return { controller, engine, clock, viewport, click, complete }
}
