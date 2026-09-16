import type { GazeEngine, GazeSample } from '../engine/gazeEngine'
import { GazeEngineUnavailableError } from '../engine/gazeEngine'
import type {
  CalibrationAttempt,
  CalibrationIssue,
  CalibrationOutcome,
  CalibrationPhase,
  CalibrationProgress,
  CalibrationSession,
  CalibrationTarget,
  EyeTrackingUnavailableReason,
  LowQualityReason,
  QualityThresholds,
  TargetPosition,
  Viewport,
} from '../types/calibration'
import type { CalibrationRecordSink } from './calibrationRecordSink'
import {
  DEFAULT_QUALITY_THRESHOLDS,
  gradeCalibration,
  validateThresholds,
  validateViewport,
} from './quality'
import { buildPointResidual, medianResidualPx, type TargetObservation } from './residuals'

export const CLICKS_PER_TARGET = 5
export const SAMPLE_FRESHNESS_MS = 700
export const DEFAULT_MAX_ATTEMPTS = 2
export const NINE_POINT_LAYOUT: readonly TargetPosition[] = Object.freeze(
  [0.16, 0.5, 0.84].flatMap((yFraction) =>
    [0.12, 0.5, 0.88].map((xFraction) => Object.freeze({ xFraction, yFraction })),
  ),
)

export interface CalibrationHandlers {
  onTargetChange?: (target: CalibrationTarget) => void | Promise<void>
  onProgress?: (progress: CalibrationProgress) => void | Promise<void>
}

export interface CalibrationControllerOptions {
  engine: GazeEngine
  session: CalibrationSession
  sink?: CalibrationRecordSink
  readViewport?: () => Viewport
  now?: () => number
  wallClock?: () => number
  createAttemptId?: () => string
  validationLayout?: readonly TargetPosition[]
  /** Total attempts, including the first; supplied by the interaction team. */
  maxAttempts?: number
  /** M5 restores this count when rebuilding a session after refresh. */
  previousAttempts?: number
  qualityThresholds?: QualityThresholds
  minSamplesPerTarget?: number
  targetSettleMs?: number
  startupTimeoutMs?: number
  onIssue?: (issue: CalibrationIssue) => void
}

interface Run {
  readonly id: string
  readonly attemptNumber: number
  readonly viewport: Viewport
  readonly startedAt: number
  readonly startedMonotonic: number
  readonly handlers: CalibrationHandlers
  readonly abort: AbortController
  readonly resolve: (attempt: CalibrationAttempt) => void
  readonly observations: Map<number, TargetObservation[]>
  readonly targets: CalibrationTarget[]
  timer?: ReturnType<typeof setTimeout>
  index: number
  clicks: number
  targetShownAt: number
  latestSample: GazeSample | null
}

/** Nine-point orchestration only: no DOM, transport, participant routes or gaze event writer. */
export class CalibrationController {
  private readonly options: CalibrationControllerOptions
  private readonly viewport: () => Viewport
  private readonly now: () => number
  private readonly wallClock: () => number
  private readonly maxAttempts: number
  private readonly minSamples: number
  private readonly settleMs: number
  private readonly startupTimeoutMs: number
  private readonly thresholds: QualityThresholds
  private readonly validationLayout: readonly TargetPosition[]
  private phaseValue: CalibrationPhase = 'IDLE'
  private attemptCount: number
  private active: Run | null = null
  private engineRunning = false
  private readonly history: CalibrationAttempt[] = []
  private readonly pending = new Map<string, CalibrationAttempt>()
  private readonly inFlight = new Set<string>()
  private readonly issuesValue: CalibrationIssue[] = []

  constructor(options: CalibrationControllerOptions) {
    if (!options.session.sessionId.trim()) throw new Error('A participant session ID is required.')
    this.options = { ...options, session: Object.freeze({ ...options.session }) }
    this.viewport =
      options.readViewport ??
      (() => ({
        width: window.innerWidth,
        height: window.innerHeight,
        devicePixelRatio: window.devicePixelRatio,
      }))
    this.now = options.now ?? (() => performance.now())
    this.wallClock = options.wallClock ?? (() => Date.now())
    this.maxAttempts = integer(options.maxAttempts ?? DEFAULT_MAX_ATTEMPTS, 'maxAttempts', 1, 10)
    this.attemptCount = integer(
      options.previousAttempts ?? 0,
      'previousAttempts',
      0,
      this.maxAttempts,
    )
    this.minSamples = integer(
      options.minSamplesPerTarget ?? 3,
      'minSamplesPerTarget',
      1,
      CLICKS_PER_TARGET,
    )
    this.settleMs = integer(options.targetSettleMs ?? 200, 'targetSettleMs', 0, 10_000)
    this.startupTimeoutMs = integer(
      options.startupTimeoutMs ?? 30_000,
      'startupTimeoutMs',
      1,
      120_000,
    )
    this.thresholds = Object.freeze({
      ...(options.qualityThresholds ?? DEFAULT_QUALITY_THRESHOLDS),
    })
    validateThresholds(this.thresholds)
    if ((options.validationLayout?.length ?? 0) > 9)
      throw new RangeError('At most nine validation targets are supported.')
    this.validationLayout = (options.validationLayout ?? []).map((position) => {
      if (
        [position.xFraction, position.yFraction].some(
          (value) => !Number.isFinite(value) || value < 0 || value > 1,
        )
      )
        throw new RangeError('Target fractions must be in [0, 1].')
      return Object.freeze({ ...position })
    })
  }

  get phase(): CalibrationPhase {
    return this.phaseValue
  }
  get running(): boolean {
    return this.phaseValue === 'RUNNING'
  }
  get attemptsMade(): number {
    return this.attemptCount
  }
  get attemptsRemaining(): number {
    return this.maxAttempts - this.attemptCount
  }
  get currentTarget(): CalibrationTarget | null {
    return this.running && this.active ? this.active.targets[this.active.index] : null
  }
  get attempts(): readonly CalibrationAttempt[] {
    return Object.freeze([...this.history])
  }
  get currentAttempt(): CalibrationAttempt | null {
    return this.history.at(-1) ?? null
  }
  get pendingRecords(): readonly CalibrationAttempt[] {
    return Object.freeze([...this.pending.values()])
  }
  get issues(): readonly CalibrationIssue[] {
    return Object.freeze([...this.issuesValue])
  }

  /** This non-prompting probe is advisory. start() still handles camera/model startup failures. */
  async probeAvailability(): Promise<EyeTrackingUnavailableReason | null> {
    this.requireConsent()
    this.requireOwnedEngine()
    try {
      return await this.options.engine.probe()
    } catch {
      return 'INIT_FAILED'
    }
  }

  /** Misuse rejects; a camera/estimator failure resolves with a session-bound result. */
  async start(handlers: CalibrationHandlers = {}): Promise<CalibrationAttempt> {
    this.requireConsent()
    this.requireOwnedEngine()
    if (this.active) throw new Error('Calibration is already starting or running.')
    if (this.attemptsRemaining === 0) throw new Error('Calibration attempt limit reached.')
    if (this.currentAttempt && !this.currentAttempt.trackingAvailable) {
      throw new Error(
        'The engine is unavailable. Continue the participant flow without eye tracking.',
      )
    }
    const viewport = Object.freeze({ ...this.viewport() })
    validateViewport(viewport)
    const startedAt = this.wallClock()
    const startedMonotonic = this.now()
    if (
      !Number.isFinite(startedAt) ||
      !Number.isFinite(startedMonotonic) ||
      Number.isNaN(new Date(startedAt).getTime())
    )
      throw new RangeError('Invalid calibration clock.')
    const id = this.options.createAttemptId?.() ?? crypto.randomUUID()
    if (!id.trim() || this.history.some((attempt) => attempt.attemptId === id)) {
      throw new Error('Attempt IDs must be non-empty and unique within the session.')
    }
    this.attemptCount += 1
    this.phaseValue = 'STARTING'
    return new Promise<CalibrationAttempt>((resolve) => {
      const run: Run = {
        id,
        attemptNumber: this.attemptCount,
        viewport,
        startedAt,
        startedMonotonic,
        handlers: { ...handlers },
        abort: new AbortController(),
        resolve,
        observations: new Map(),
        targets: [
          ...targets(NINE_POINT_LAYOUT, 'CALIBRATION', viewport),
          ...targets(this.validationLayout, 'VALIDATION', viewport),
        ],
        index: 0,
        clicks: 0,
        targetShownAt: startedMonotonic,
        latestSample: null,
      }
      this.active = run
      run.timer = setTimeout(() => this.cancelStartup(run, 'INIT_TIMEOUT'), this.startupTimeoutMs)
      void this.initialize(run)
    })
  }

  retry(handlers: CalibrationHandlers = {}): Promise<CalibrationAttempt> {
    return this.start(handlers)
  }

  /**
   * UI supplies the actual dot centre on the first click if its layout differs.
   * The centre stays fixed for that target; use checkViewport() when the layout changes.
   */
  registerClick(targetX?: number, targetY?: number): void {
    const run = this.active
    if (!this.running || !run) return
    if (!this.checkViewport()) return
    let target = run.targets[run.index]
    const x = targetX ?? target.targetX
    const y = targetY ?? target.targetY
    if (
      !Number.isFinite(x) ||
      !Number.isFinite(y) ||
      x < 0 ||
      y < 0 ||
      x > run.viewport.width ||
      y > run.viewport.height
    )
      throw new RangeError('Target centre is outside the viewport.')
    if (run.clicks > 0 && (x !== target.targetX || y !== target.targetY)) {
      this.finish(run, 'ABANDONED', null, ['VIEWPORT_CHANGED'])
      return
    }
    target = Object.freeze({ ...target, targetX: x, targetY: y })
    run.targets[run.index] = target
    const time = this.now()
    // Early clicks do not train, consume samples or advance the displayed target.
    if (time < run.targetShownAt + this.settleMs) return
    const sample = run.latestSample
    run.latestSample = null
    try {
      if (target.kind === 'CALIBRATION') this.options.engine.recordScreenPosition(x, y)
    } catch (error) {
      this.report({ stage: 'engine', error, attemptId: run.id })
      this.stopEngine()
      this.finish(run, 'ABANDONED', 'INIT_FAILED')
      return
    }
    const observations = run.observations.get(run.index) ?? []
    if (
      sample &&
      sample.timestamp >= run.targetShownAt + this.settleMs &&
      sample.timestamp <= time &&
      time - sample.timestamp < SAMPLE_FRESHNESS_MS
    ) {
      observations.push({ x: sample.x, y: sample.y })
      run.observations.set(run.index, observations)
    }
    run.clicks += 1
    const done = run.clicks === target.clicksRequired
    this.notify(() =>
      run.handlers.onProgress?.(
        Object.freeze({
          target,
          clicksRecorded: run.clicks,
          validSamples: observations.length,
          targetsCompleted: run.index + (done ? 1 : 0),
          targetsTotal: run.targets.length,
        }),
      ),
    )
    if (this.active !== run || !done) return
    run.index += 1
    run.clicks = 0
    if (run.index === run.targets.length) this.finish(run, 'COMPLETED')
    else this.showTarget(run)
  }

  /** The UI can call this on resize/zoom; registerClick also checks automatically. */
  checkViewport(): boolean {
    const run = this.active
    if (!run) return false
    try {
      const current = this.viewport()
      validateViewport(current)
      if (
        current.width === run.viewport.width &&
        current.height === run.viewport.height &&
        current.devicePixelRatio === run.viewport.devicePixelRatio
      )
        return true
    } catch {
      /* An invalid viewport also invalidates the measurement. */
    }
    if (this.phaseValue === 'STARTING') this.cancelStartup(run, 'INIT_CANCELLED')
    else this.finish(run, 'ABANDONED', null, ['VIEWPORT_CHANGED'])
    return false
  }

  skip(): void {
    const run = this.active
    if (!run) return
    if (this.phaseValue === 'STARTING') this.cancelStartup(run, 'INIT_CANCELLED')
    else this.finish(run, 'ABANDONED')
  }

  /** FR-50 becomes the owner and must set its listener and stop the engine at session end. */
  handoffToTracking(): GazeEngine {
    if (
      this.phaseValue !== 'FINISHED' ||
      !this.engineRunning ||
      !this.currentAttempt?.trackingAvailable
    ) {
      throw new Error('Only a finished attempt with an available engine can enter eye tracking.')
    }
    this.phaseValue = 'HANDED_OFF'
    return this.options.engine
  }

  /** Leaving calibration before handoff stops the session engine and settles any pending run. */
  dispose(): void {
    if (this.phaseValue === 'DISPOSED') return
    const handedOff = this.phaseValue === 'HANDED_OFF'
    const run = this.active
    this.phaseValue = 'DISPOSED'
    if (!handedOff) this.stopEngine()
    if (run) {
      run.abort.abort()
      this.finish(run, 'ABANDONED', 'INIT_CANCELLED')
    }
  }

  /** Retry only the handoff, with the same frozen record/ID; this never repeats calibration. */
  retryPendingRecords(): void {
    for (const attempt of this.pending.values()) this.deliver(attempt)
  }

  private async initialize(run: Run): Promise<void> {
    try {
      const reason = await this.options.engine.probe()
      if (this.active !== run) return
      if (reason !== null) {
        this.stopEngine()
        this.finish(run, 'UNAVAILABLE', reason)
        return
      }
      this.options.engine.setSampleListener(null)
      await this.options.engine.clearTrainingData()
      if (this.active !== run) return
      await this.options.engine.start((sample) => {
        if (
          this.active === run &&
          this.running &&
          Number.isFinite(sample.x) &&
          Number.isFinite(sample.y) &&
          Number.isFinite(sample.timestamp)
        ) {
          run.latestSample = { ...sample }
        }
      }, run.abort.signal)
      if (this.active !== run) {
        this.stopEngine()
        return
      }
      this.engineRunning = true
      clearTimeout(run.timer)
      this.phaseValue = 'RUNNING'
      this.showTarget(run)
    } catch (error) {
      if (this.active !== run) return
      this.report({ stage: 'engine', error, attemptId: run.id })
      this.stopEngine()
      this.finish(
        run,
        'UNAVAILABLE',
        error instanceof GazeEngineUnavailableError ? error.reason : 'INIT_FAILED',
      )
    }
  }

  private showTarget(run: Run): void {
    run.latestSample = null
    run.targetShownAt = this.now()
    this.notify(() => run.handlers.onTargetChange?.(run.targets[run.index]))
  }

  private cancelStartup(run: Run, reason: EyeTrackingUnavailableReason): void {
    if (this.active !== run || this.phaseValue !== 'STARTING') return
    run.abort.abort()
    this.stopEngine()
    this.finish(run, 'UNAVAILABLE', reason)
  }

  private finish(
    run: Run,
    outcome: CalibrationOutcome,
    unavailableReason: EyeTrackingUnavailableReason | null = null,
    extraReasons: LowQualityReason[] = [],
  ): void {
    if (this.active !== run) return
    this.active = null
    clearTimeout(run.timer)
    if (this.phaseValue !== 'DISPOSED') this.phaseValue = 'FINISHED'
    try {
      this.options.engine.setSampleListener(null)
    } catch (error) {
      this.report({ stage: 'engine', error, attemptId: run.id })
    }
    const pointResiduals = Object.freeze(
      run.targets.flatMap((target, index) => {
        const result = buildPointResidual(target, run.observations.get(index) ?? [])
        return result ? [result] : []
      }),
    )
    const calibration = pointResiduals.filter((point) => point.pointKind === 'CALIBRATION')
    const validation = pointResiduals.filter((point) => point.pointKind === 'VALIDATION')
    const residualMedianPx = medianResidualPx(calibration)
    const validationMedianPx = medianResidualPx(validation)
    const calibrationTargetsMeasured = calibration.filter(
      (point) => point.sampleCount >= this.minSamples,
    ).length
    const validationTargetsMeasured = validation.filter(
      (point) => point.sampleCount >= this.minSamples,
    ).length
    const score = this.validationLayout.length ? validationMedianPx : residualMedianPx
    let quality = gradeCalibration(score, run.viewport.width, run.viewport.height, this.thresholds)
    const reasons = new Set<LowQualityReason>(extraReasons)
    if (outcome === 'ABANDONED') reasons.add('ABANDONED')
    if (unavailableReason) reasons.add('ENGINE_UNAVAILABLE')
    if (quality === 'NOT_MEASURED') reasons.add('NOT_MEASURED')
    if (quality === 'POOR') reasons.add('HIGH_RESIDUAL')
    if (
      calibrationTargetsMeasured < NINE_POINT_LAYOUT.length ||
      validationTargetsMeasured < this.validationLayout.length
    )
      reasons.add('INSUFFICIENT_SAMPLES')
    if (quality !== 'NOT_MEASURED' && reasons.size > 0) quality = 'POOR'
    const elapsed = Math.max(0, this.now() - run.startedMonotonic)
    const finishedAt = run.startedAt + (Number.isFinite(elapsed) ? elapsed : 0)
    const attempt: CalibrationAttempt = Object.freeze({
      schemaVersion: 1,
      algorithmVersion: 'residual-v2',
      sessionId: this.options.session.sessionId,
      attemptId: run.id,
      attemptNumber: run.attemptNumber,
      outcome,
      trackingAvailable: this.engineRunning && unavailableReason === null,
      unavailableReason,
      quality,
      qualityBasis:
        score === null ? 'NONE' : this.validationLayout.length ? 'VALIDATION' : 'CALIBRATION',
      lowQualityReasons: Object.freeze([...reasons]),
      qualityThresholds: this.thresholds,
      minSamplesPerTarget: this.minSamples,
      calibrationTargetsMeasured,
      calibrationTargetsRequired: NINE_POINT_LAYOUT.length,
      validationTargetsMeasured,
      validationTargetsRequired: this.validationLayout.length,
      residualMedianPx,
      validationMedianPx,
      viewportWidth: run.viewport.width,
      viewportHeight: run.viewport.height,
      devicePixelRatio: run.viewport.devicePixelRatio,
      startedAt: new Date(run.startedAt).toISOString(),
      finishedAt: new Date(finishedAt).toISOString(),
      pointResiduals,
    })
    this.history.push(attempt)
    this.pending.set(attempt.attemptId, attempt)
    run.resolve(attempt)
    this.deliver(attempt)
  }

  private deliver(attempt: CalibrationAttempt): void {
    const sink = this.options.sink
    if (!sink || this.inFlight.has(attempt.attemptId)) return
    this.inFlight.add(attempt.attemptId)
    void Promise.resolve()
      .then(() => sink.record(attempt))
      .then(() => {
        this.pending.delete(attempt.attemptId)
      })
      .catch((error: unknown) => {
        this.report({ stage: 'recording', error, attemptId: attempt.attemptId })
      })
      .finally(() => {
        this.inFlight.delete(attempt.attemptId)
      })
  }

  private stopEngine(): void {
    this.engineRunning = false
    try {
      this.options.engine.stop()
    } catch (error) {
      this.report({ stage: 'engine', error })
    }
  }

  private notify(callback: () => void | Promise<void>): void {
    try {
      void Promise.resolve(callback()).catch((error: unknown) =>
        this.report({ stage: 'handler', error }),
      )
    } catch (error) {
      this.report({ stage: 'handler', error })
    }
  }

  private report(issue: CalibrationIssue): void {
    // Bound diagnostics; finished attempt records themselves are never silently evicted.
    if (this.issuesValue.length === 50) this.issuesValue.shift()
    this.issuesValue.push(Object.freeze(issue))
    try {
      void Promise.resolve(this.options.onIssue?.(issue)).catch(() => {})
    } catch {
      /* Diagnostics cannot interrupt the participant. */
    }
  }

  private requireConsent(): void {
    if (!this.options.session.consentGiven || !this.options.session.eyeTrackingEnabled) {
      throw new Error('Calibration requires consent and an eye-tracking-enabled study.')
    }
  }

  private requireOwnedEngine(): void {
    if (this.phaseValue === 'DISPOSED' || this.phaseValue === 'HANDED_OFF') {
      throw new Error('This controller no longer owns the eye-tracking engine.')
    }
  }
}

function integer(value: number, name: string, min: number, max: number): number {
  if (!Number.isInteger(value) || value < min || value > max) {
    throw new RangeError(`${name} must be an integer between ${min} and ${max}.`)
  }
  return value
}

function targets(
  layout: readonly TargetPosition[],
  kind: CalibrationTarget['kind'],
  viewport: Viewport,
): CalibrationTarget[] {
  return layout.map((position, index) =>
    Object.freeze({
      kind,
      index,
      targetX: Math.round(position.xFraction * viewport.width),
      targetY: Math.round(position.yFraction * viewport.height),
      clicksRequired: CLICKS_PER_TARGET,
    }),
  )
}
