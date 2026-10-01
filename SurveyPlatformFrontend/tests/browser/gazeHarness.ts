import {
  GazeEventCollector,
  browserGazeEnvironment,
  collectGazeBatches,
  createGazeBufferPort,
  createMainThreadBufferPort,
  createSessionClock,
} from '../../src/eyetracking'
import type {
  CalibrationAttempt,
  GazeEngine,
  GazeEvent,
  GazeEventBatch,
  GazeSample,
} from '../../src/eyetracking'

const WALL_ORIGIN = 1_700_000_000_000

/** Stands in for the engine FR-49 hands over. It never touches a camera. */
class HarnessEngine implements GazeEngine {
  listener: ((sample: GazeSample) => void) | null = null
  stopped = false
  async probe(): Promise<null> {
    return null
  }
  async start(onSample: (sample: GazeSample) => void): Promise<void> {
    this.listener = onSample
  }
  setSampleListener(onSample: ((sample: GazeSample) => void) | null): void {
    this.listener = onSample
  }
  recordScreenPosition(): void {}
  clearTrainingData(): void {}
  stop(): void {
    this.stopped = true
    this.listener = null
  }
}

const calibration: CalibrationAttempt = Object.freeze({
  schemaVersion: 1,
  algorithmVersion: 'residual-v2',
  sessionId: 'browser-session',
  attemptId: 'attempt-1',
  attemptNumber: 1,
  outcome: 'COMPLETED',
  trackingAvailable: true,
  unavailableReason: null,
  quality: 'FAIR',
  qualityBasis: 'CALIBRATION',
  lowQualityReasons: Object.freeze(['HIGH_RESIDUAL' as const]),
  qualityThresholds: Object.freeze({ goodFraction: 0.05, fairFraction: 0.1 }),
  minSamplesPerTarget: 3,
  calibrationTargetsMeasured: 9,
  calibrationTargetsRequired: 9,
  validationTargetsMeasured: 0,
  validationTargetsRequired: 0,
  residualMedianPx: 64,
  validationMedianPx: null,
  viewportWidth: 1280,
  viewportHeight: 720,
  devicePixelRatio: 1,
  startedAt: new Date(WALL_ORIGIN - 30_000).toISOString(),
  finishedAt: new Date(WALL_ORIGIN).toISOString(),
  pointResiduals: Object.freeze([]),
})

const engine = new HarnessEngine()
const sink = collectGazeBatches('browser-session')
let clock = 0
let collector: GazeEventCollector | null = null

const status = document.getElementById('status')!
const dot = document.getElementById('dot')!

function centre(selector: string) {
  const element = document.querySelector(selector)
  if (!element) throw new Error(`The harness layout is missing ${selector}.`)
  const box = element.getBoundingClientRect()
  return { x: Math.round(box.left + box.width / 2), y: Math.round(box.top + box.height / 2) }
}

function render(): void {
  if (!collector) return
  const stats = collector.stats
  const last = sink.events.at(-1)
  status.textContent = [
    `buffer: ${collector.bufferMode ?? 'not started'}   phase: ${collector.phase}`,
    `samples ${stats.samplesReceived}  events ${stats.eventsProduced}  discarded ${stats.eventsDiscarded}`,
    `batches ok ${stats.batchesDelivered}  failed ${stats.batchesFailed}`,
    `widget ${stats.attribution.WIDGET}  no-content ${stats.attribution.NO_CONTENT}  unresolved ${stats.attribution.UNRESOLVED}`,
    last ? `last: ${last.attribution} ${last.widgetId ?? '-'} @ ${last.x},${last.y}` : 'last: -',
  ].join('\n')
  if (last) {
    dot.style.display = 'block'
    dot.style.left = `${last.x}px`
    dot.style.top = `${last.y}px`
  }
}

const harness = {
  start(options: { mainThread?: boolean; batchSize?: number; maxEvents?: number } = {}) {
    collector = new GazeEventCollector({
      engine,
      sink,
      session: { sessionId: 'browser-session', consentGiven: true, eyeTrackingEnabled: true },
      calibration,
      clock: createSessionClock({ now: () => clock, wallClock: () => WALL_ORIGIN }),
      pageId: 'page-1',
      environment: browserGazeEnvironment(window),
      createBufferPort: options.mainThread ? createMainThreadBufferPort : createGazeBufferPort,
      batchSize: options.batchSize ?? 4,
      maxEvents: options.maxEvents,
      flushIntervalMs: 0,
    })
    collector.start()
    render()
  },
  emit(x: number, y: number, step = 20) {
    clock += step
    engine.listener?.({ x, y, timestamp: clock })
    render()
  },
  /** Measures only the synchronous main-thread cost the collector adds per prediction. */
  emitMany(count: number) {
    let mainThreadMs = 0
    for (let index = 0; index < count; index += 1) {
      clock += 16
      const started = performance.now()
      engine.listener?.({ x: 40 + (index % 900), y: 90 + (index % 400), timestamp: clock })
      mainThreadMs += performance.now() - started
    }
    render()
    return { count, mainThreadMs, perSampleMs: mainThreadMs / count }
  },
  flush() {
    collector?.flushNow()
    render()
  },
  async stop() {
    await collector?.stop()
    render()
  },
  points() {
    return {
      widget: centre('[data-widget-id="question-2"] .leaf'),
      chrome: centre('#chrome'),
      unmarked: centre('#unmarked .leaf'),
      offscreen: { x: -20, y: -20 },
    }
  },
  async settle(turns = 40) {
    for (let index = 0; index < turns; index += 1) await new Promise((r) => setTimeout(r, 0))
    render()
  },
  state() {
    const batches = sink.batches as GazeEventBatch[]
    return {
      mode: collector?.bufferMode ?? null,
      phase: collector?.phase ?? null,
      stats: collector?.stats ?? null,
      issues: (collector?.issues ?? []).map((issue) => issue.stage),
      batches: batches.map((batch) => ({
        schemaVersion: batch.schemaVersion,
        sessionId: batch.sessionId,
        batchId: batch.batchId,
        sequence: batch.sequence,
        eventCount: batch.eventCount,
        droppedEvents: batch.droppedEvents,
        droppedEventsTotal: batch.droppedEventsTotal,
      })),
      payloads: batches.map((batch) => batch.payload),
      events: sink.events as GazeEvent[],
      engineStopped: engine.stopped,
    }
  },
}

declare global {
  interface Window {
    fr50Harness: typeof harness
  }
}
window.fr50Harness = harness
render()
