import type { GazeEngine, GazeSample } from '../engine/gazeEngine'
import type { CalibrationAttempt, CalibrationSession } from '../types/calibration'
import type { GazeEventSink } from './gazeEventSink'
import type { SessionClock } from './sessionClock'
import type {
  GazeAttribution,
  GazeBufferMode,
  GazeCollectorStats,
  GazeEvent,
  GazeEventBatch,
  GazeIssue,
  GazeViewport,
} from './types'
import {
  attributeGaze,
  browserGazeEnvironment,
  UNRESOLVED_ATTRIBUTION,
  type GazeEnvironment,
  type WidgetAttributionResult,
} from './widgetAttribution'
import {
  createGazeBufferPort,
  type GazeBufferPort,
  type GazeBufferPortFactory,
} from './worker/workerBuffer'

export const DEFAULT_FLUSH_INTERVAL_MS = 2000
export const DEFAULT_DRAIN_TIMEOUT_MS = 5000

export type GazeCollectorPhase = 'IDLE' | 'COLLECTING' | 'STOPPING' | 'STOPPED'

export interface GazeEventCollectorOptions {
  /** Taken over from CalibrationController.handoffToTracking(); this collector now stops it. */
  engine: GazeEngine
  sink: GazeEventSink
  session: CalibrationSession
  /** Supplies the calibration quality identifiers stamped onto every event. */
  calibration: CalibrationAttempt
  /** Shared with behavioural collection, so both streams sit on one time axis. */
  clock: SessionClock
  pageId: string
  environment?: GazeEnvironment
  createBufferPort?: GazeBufferPortFactory
  maxEvents?: number
  batchSize?: number
  /** 0 disables the periodic cut; the caller then drives flushNow(). */
  flushIntervalMs?: number
  drainTimeoutMs?: number
  onIssue?: (issue: GazeIssue) => void
}

interface Counters {
  samplesReceived: number
  samplesRejected: number
  eventsProduced: number
  buffered: number
  overflowDiscarded: number
  lostDiscarded: number
  rejectedBySink: number
  batchesDelivered: number
  batchesFailed: number
  attribution: Record<GazeAttribution, number>
}

/**
 * Turns every valid prediction into one complete gaze event and hands it to the shared pipeline.
 *
 * What stays on the main thread is one elementFromPoint call, one layout read and one postMessage
 * per prediction, because attribution needs the DOM and a worker has none. Buffering, batching and
 * serialization happen off the main thread whenever a module worker can be created.
 *
 * Not here: injecting data-widget-id, transporting or storing events, behavioural events, session
 * lifecycle, and any derived measure such as dwell time or fixation density.
 */
export class GazeEventCollector {
  private readonly options: GazeEventCollectorOptions
  private readonly environment: GazeEnvironment
  private readonly clock: SessionClock
  private readonly sessionId: string
  private readonly flushIntervalMs: number
  private readonly drainTimeoutMs: number
  private readonly counters: Counters = {
    samplesReceived: 0,
    samplesRejected: 0,
    eventsProduced: 0,
    buffered: 0,
    overflowDiscarded: 0,
    lostDiscarded: 0,
    rejectedBySink: 0,
    batchesDelivered: 0,
    batchesFailed: 0,
    attribution: { WIDGET: 0, NO_CONTENT: 0, UNRESOLVED: 0 },
  }
  private readonly issuesValue: GazeIssue[] = []
  private phaseValue: GazeCollectorPhase = 'IDLE'
  private currentPageId: string
  private port: GazeBufferPort | null = null
  private bufferModeValue: GazeBufferMode | null = null
  private flushTimer: ReturnType<typeof setInterval> | null = null
  private drainTimer: ReturnType<typeof setTimeout> | null = null
  private drainResolve: (() => void) | null = null
  private stopping: Promise<void> | null = null

  constructor(options: GazeEventCollectorOptions) {
    const sessionId = options.session.sessionId
    if (!sessionId.trim()) throw new Error('A participant session ID is required.')
    if (!options.session.consentGiven || !options.session.eyeTrackingEnabled) {
      throw new Error('Gaze collection requires consent and an eye-tracking-enabled study.')
    }
    if (options.calibration.sessionId !== sessionId) {
      throw new Error('The calibration attempt belongs to a different participant session.')
    }
    if (!options.calibration.trackingAvailable) {
      throw new Error('Gaze collection needs a calibration attempt with an available engine.')
    }
    this.options = { ...options }
    this.sessionId = sessionId
    this.clock = options.clock
    this.currentPageId = requirePageId(options.pageId)
    this.environment = options.environment ?? browserGazeEnvironment(requireWindow())
    this.flushIntervalMs = integer(
      options.flushIntervalMs ?? DEFAULT_FLUSH_INTERVAL_MS,
      'flushIntervalMs',
      0,
      600_000,
    )
    this.drainTimeoutMs = integer(
      options.drainTimeoutMs ?? DEFAULT_DRAIN_TIMEOUT_MS,
      'drainTimeoutMs',
      1,
      600_000,
    )
  }

  get phase(): GazeCollectorPhase {
    return this.phaseValue
  }
  get pageId(): string {
    return this.currentPageId
  }
  /** Null until start(); then the path in use, and after stop() the last path that was used. */
  get bufferMode(): GazeBufferMode | null {
    return this.port?.mode ?? this.bufferModeValue
  }
  get issues(): readonly GazeIssue[] {
    return Object.freeze([...this.issuesValue])
  }
  get stats(): GazeCollectorStats {
    const counters = this.counters
    return Object.freeze({
      samplesReceived: counters.samplesReceived,
      samplesRejected: counters.samplesRejected,
      eventsProduced: counters.eventsProduced,
      eventsDiscarded:
        counters.overflowDiscarded + counters.lostDiscarded + counters.rejectedBySink,
      eventsBuffered: counters.buffered,
      batchesDelivered: counters.batchesDelivered,
      batchesFailed: counters.batchesFailed,
      attribution: Object.freeze({ ...counters.attribution }),
    })
  }

  start(): void {
    if (this.phaseValue !== 'IDLE') throw new Error('The gaze collector has already been started.')
    this.phaseValue = 'COLLECTING'
    const factory = this.options.createBufferPort ?? createGazeBufferPort
    this.port = factory(
      {
        sessionId: this.sessionId,
        maxEvents: this.options.maxEvents,
        batchSize: this.options.batchSize,
      },
      {
        onBatch: (batch) => this.handleBatch(batch),
        onDrained: () => this.settleDrain(),
        onStats: (stats) => {
          this.counters.buffered = stats.pendingCount
          if (stats.droppedEventsTotal > this.counters.overflowDiscarded) {
            this.counters.overflowDiscarded = stats.droppedEventsTotal
          }
        },
        onIssue: (error) => this.report({ stage: 'buffer', error }),
        onDiscard: (count) => {
          if (Number.isInteger(count) && count > 0) this.counters.lostDiscarded += count
        },
      },
    )
    this.options.engine.setSampleListener((sample) => this.onSample(sample))
    if (this.flushIntervalMs > 0) {
      this.flushTimer = setInterval(() => this.flushNow(), this.flushIntervalMs)
    }
  }

  /** Call on navigation inside the session; later events carry the new page. */
  setPage(pageId: string): void {
    this.currentPageId = requirePageId(pageId)
  }

  /** Cuts a partial batch now. The periodic cadence calls this; tests and hosts may too. */
  flushNow(): void {
    this.port?.flush()
  }

  /**
   * Stops the engine first so no further predictions arrive, then empties the buffer. Resolving
   * means every event this collector accepted has been offered to the sink exactly once.
   */
  stop(): Promise<void> {
    if (this.stopping) return this.stopping
    if (this.phaseValue === 'IDLE' || this.phaseValue === 'STOPPED') {
      // Stopping a collector that never collected still releases the engine it was handed.
      if (this.phaseValue === 'IDLE') this.stopEngine()
      this.phaseValue = 'STOPPED'
      this.stopping = Promise.resolve()
      return this.stopping
    }
    this.phaseValue = 'STOPPING'
    if (this.flushTimer !== null) {
      clearInterval(this.flushTimer)
      this.flushTimer = null
    }
    this.stopEngine()
    this.stopping = this.drain().then(() => {
      this.bufferModeValue = this.port?.mode ?? this.bufferModeValue
      this.port?.close()
      this.port = null
      this.phaseValue = 'STOPPED'
    })
    return this.stopping
  }

  private stopEngine(): void {
    try {
      this.options.engine.setSampleListener(null)
    } catch (error) {
      this.report({ stage: 'engine', error })
    }
    try {
      this.options.engine.stop()
    } catch (error) {
      this.report({ stage: 'engine', error })
    }
  }

  private onSample(sample: GazeSample): void {
    if (this.phaseValue !== 'COLLECTING' || !this.port) return
    this.counters.samplesReceived += 1
    if (
      !Number.isFinite(sample.x) ||
      !Number.isFinite(sample.y) ||
      !Number.isFinite(sample.timestamp)
    ) {
      this.counters.samplesRejected += 1
      return
    }
    let viewport: GazeViewport
    try {
      viewport = this.environment.readViewport()
    } catch (error) {
      this.counters.samplesRejected += 1
      this.report({ stage: 'viewport', error })
      return
    }
    if (
      ![
        viewport.width,
        viewport.height,
        viewport.devicePixelRatio,
        viewport.scrollX,
        viewport.scrollY,
      ].every((value) => Number.isFinite(value))
    ) {
      this.counters.samplesRejected += 1
      this.report({ stage: 'viewport', error: new Error('The viewport reading is not finite.') })
      return
    }
    // Attribution failure annotates the event; it never discards it.
    let attribution: WidgetAttributionResult = UNRESOLVED_ATTRIBUTION
    try {
      attribution = attributeGaze(this.environment, sample.x, sample.y)
    } catch (error) {
      this.report({ stage: 'attribution', error })
    }
    let timestamp: string
    let sessionElapsedMs: number
    try {
      const reading = this.clock.readingFrom(sample.timestamp)
      timestamp = reading.iso
      sessionElapsedMs = reading.sessionElapsedMs
    } catch (error) {
      // Without a valid time the record cannot sit on the shared axis, so it is refused, not faked.
      this.counters.samplesRejected += 1
      this.report({ stage: 'clock', error })
      return
    }
    const calibration = this.options.calibration
    const event: GazeEvent = Object.freeze({
      schemaVersion: 1,
      sessionId: this.sessionId,
      pageId: this.currentPageId,
      x: sample.x,
      y: sample.y,
      scrollX: viewport.scrollX,
      scrollY: viewport.scrollY,
      pageX: sample.x + viewport.scrollX,
      pageY: sample.y + viewport.scrollY,
      viewportWidth: viewport.width,
      viewportHeight: viewport.height,
      devicePixelRatio: viewport.devicePixelRatio,
      widgetId: attribution.widgetId,
      attribution: attribution.attribution,
      timestamp,
      sessionElapsedMs,
      calibrationAttemptId: calibration.attemptId,
      calibrationQuality: calibration.quality,
      calibrationLowQualityReasons: calibration.lowQualityReasons,
    })
    this.counters.eventsProduced += 1
    this.counters.attribution[attribution.attribution] += 1
    try {
      this.port.add(event)
    } catch (error) {
      this.report({ stage: 'buffer', error })
    }
  }

  private handleBatch(batch: GazeEventBatch): void {
    if (batch.droppedEventsTotal > this.counters.overflowDiscarded) {
      this.counters.overflowDiscarded = batch.droppedEventsTotal
    }
    void Promise.resolve()
      .then(() => this.options.sink.deliver(batch))
      .then(() => {
        this.counters.batchesDelivered += 1
      })
      .catch((error: unknown) => {
        this.counters.batchesFailed += 1
        this.counters.rejectedBySink += batch.eventCount
        this.report({ stage: 'delivery', error })
      })
      .finally(() => {
        // Released whatever the outcome: a stalled pipeline would lose the rest of the session.
        this.port?.release()
      })
  }

  private drain(): Promise<void> {
    const port = this.port
    if (!port) return Promise.resolve()
    return new Promise<void>((resolve) => {
      this.drainResolve = resolve
      this.drainTimer = setTimeout(() => {
        this.report({
          stage: 'drain',
          error: new Error('Timed out draining the gaze buffer; queued events were not delivered.'),
        })
        this.settleDrain()
      }, this.drainTimeoutMs)
      port.drain()
    })
  }

  private settleDrain(): void {
    if (!this.drainResolve) return
    if (this.drainTimer !== null) {
      clearTimeout(this.drainTimer)
      this.drainTimer = null
    }
    const resolve = this.drainResolve
    this.drainResolve = null
    resolve()
  }

  private report(issue: GazeIssue): void {
    // Bounded diagnostics. Event loss is counted in stats, never only here.
    if (this.issuesValue.length === 50) this.issuesValue.shift()
    this.issuesValue.push(Object.freeze(issue))
    try {
      this.options.onIssue?.(issue)
    } catch {
      /* Diagnostics cannot interrupt the participant. */
    }
  }
}

function requirePageId(pageId: string): string {
  if (!pageId.trim()) throw new Error('A page ID is required for every gaze event.')
  return pageId
}

function requireWindow(): Window {
  const view = globalThis.window
  if (!view) throw new Error('A GazeEnvironment is required outside a browser window.')
  return view
}

function integer(value: number, name: string, min: number, max: number): number {
  if (!Number.isInteger(value) || value < min || value > max) {
    throw new RangeError(`${name} must be an integer between ${min} and ${max}.`)
  }
  return value
}
