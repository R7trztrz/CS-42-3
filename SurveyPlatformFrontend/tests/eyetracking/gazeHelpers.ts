import {
  GazeEventCollector,
  collectGazeBatches,
  createMainThreadBufferPort,
  createSessionClock,
} from '../../src/eyetracking'
import type {
  AttributableElement,
  CalibrationAttempt,
  GazeEnvironment,
  GazeEventCollectorOptions,
  GazeViewport,
} from '../../src/eyetracking'
import { FakeEngine } from './helpers'

export const WALL_ORIGIN = 1_700_000_000_000

export function attemptFixture(overrides: Partial<CalibrationAttempt> = {}): CalibrationAttempt {
  return Object.freeze({
    schemaVersion: 1,
    algorithmVersion: 'residual-v2',
    sessionId: 'session-A',
    attemptId: 'attempt-1',
    attemptNumber: 1,
    outcome: 'COMPLETED',
    trackingAvailable: true,
    unavailableReason: null,
    quality: 'GOOD',
    qualityBasis: 'CALIBRATION',
    lowQualityReasons: Object.freeze([]),
    qualityThresholds: Object.freeze({ goodFraction: 0.05, fairFraction: 0.1 }),
    minSamplesPerTarget: 3,
    calibrationTargetsMeasured: 9,
    calibrationTargetsRequired: 9,
    validationTargetsMeasured: 0,
    validationTargetsRequired: 0,
    residualMedianPx: 12,
    validationMedianPx: null,
    viewportWidth: 1000,
    viewportHeight: 800,
    devicePixelRatio: 2,
    startedAt: '2026-09-21T00:00:00.000Z',
    finishedAt: '2026-09-21T00:00:30.000Z',
    pointResiduals: Object.freeze([]),
    ...overrides,
  })
}

/** A real Element satisfies AttributableElement structurally; these fakes keep tests DOM-free. */
export function fakeElement(
  widgetId: string | null,
  parent: AttributableElement | null = null,
): AttributableElement {
  return {
    getAttribute: (name) => (name === 'data-widget-id' ? widgetId : null),
    parentElement: parent,
  }
}

export function fakeEnvironment(initial: Partial<GazeViewport> = {}) {
  let viewport: GazeViewport = {
    width: 1000,
    height: 800,
    devicePixelRatio: 2,
    scrollX: 0,
    scrollY: 0,
    ...initial,
  }
  let hit: (x: number, y: number) => AttributableElement | null = () => null
  const environment: GazeEnvironment = {
    readViewport: () => viewport,
    elementFromPoint: (x, y) => hit(x, y),
  }
  return {
    environment,
    setViewport(next: Partial<GazeViewport>) {
      viewport = { ...viewport, ...next }
    },
    setHit(next: (x: number, y: number) => AttributableElement | null) {
      hit = next
    },
  }
}

/** More turns than the FR-49 helper, because a batch crosses several microtask hops. */
export async function settle(turns = 60) {
  for (let i = 0; i < turns; i += 1) await Promise.resolve()
}

export function setupCollector(options: Partial<GazeEventCollectorOptions> = {}) {
  const engine = new FakeEngine()
  const clock = { time: 0 }
  const sessionClock = createSessionClock({
    now: () => clock.time,
    wallClock: () => WALL_ORIGIN,
  })
  const sink = collectGazeBatches('session-A')
  const world = fakeEnvironment()
  const collector = new GazeEventCollector({
    engine,
    sink,
    session: { sessionId: 'session-A', consentGiven: true, eyeTrackingEnabled: true },
    calibration: attemptFixture(),
    clock: sessionClock,
    pageId: 'page-1',
    environment: world.environment,
    // The worker path is exercised in the browser tests; here the shared host runs in-thread.
    createBufferPort: createMainThreadBufferPort,
    flushIntervalMs: 0,
    batchSize: 2,
    ...options,
  })
  const emit = (x: number, y: number, step = 20) => {
    clock.time += step
    engine.emit(x, y, clock.time)
  }
  return { collector, engine, sink, clock, sessionClock, world, emit }
}
