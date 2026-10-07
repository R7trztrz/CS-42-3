import { CalibrationController, DEFAULT_MAX_ATTEMPTS } from '../calibration/calibrationController'
import { isRetryWorthOffering } from '../calibration/quality'
import type { GazeEngine } from '../engine/gazeEngine'
import { GazeEngineUnavailableError } from '../engine/gazeEngine'
import { WebGazerGazeEngine } from '../engine/webgazerGazeEngine'
import { GazeEventCollector } from '../gaze/gazeEventCollector'
import { createSessionClock, type SessionClock } from '../gaze/sessionClock'
import type { GazeCollectorStats } from '../gaze/types'
import type { GazeEnvironment } from '../gaze/widgetAttribution'
import type { GazeBufferPortFactory } from '../gaze/worker/workerBuffer'
import type {
  CalibrationAttempt,
  CalibrationProgress,
  CalibrationTarget,
  EyeTrackingUnavailableReason,
  Viewport,
} from '../types/calibration'
import type { EyeTrackingTransport } from './eyeTrackingTransport'
import { loadWebGazer, type LoadedWebGazer } from './webgazerLoader'

/** FR-49 default: the first attempt plus one recalibration. */
export const PARTICIPANT_MAX_CALIBRATION_ATTEMPTS = DEFAULT_MAX_ATTEMPTS
/** Passed to the controller and mirrored by the UI, which disables a target for this long. */
export const CALIBRATION_TARGET_SETTLE_MS = 200
/** The participant page renders one feed, so all browsing gaze shares one page id. */
export const BROWSING_PAGE_ID = 'feed'
/** How long leaving the browsing phase waits for queued gaze batches to be stored. */
export const GAZE_DRAIN_TIMEOUT_MS = 8_000

/**
 * IDLE: nothing loaded and no camera. STARTING: loading WebGazer and the camera. CALIBRATING:
 * targets on screen. FINISHED: an attempt ended. TRACKING: browsing gaze is being collected.
 * STOPPING: queued gaze is being delivered. STOPPED: the camera is released for this page.
 */
export type ParticipantEyeTrackingStage =
  | 'IDLE'
  | 'STARTING'
  | 'CALIBRATING'
  | 'FINISHED'
  | 'TRACKING'
  | 'STOPPING'
  | 'STOPPED'

export interface ParticipantEyeTrackingSnapshot {
  readonly stage: ParticipantEyeTrackingStage
  /** The target to draw while CALIBRATING, in viewport CSS pixels. */
  readonly target: CalibrationTarget | null
  readonly progress: CalibrationProgress | null
  /** The latest finished attempt; the one browsing continues with. */
  readonly attempt: CalibrationAttempt | null
  readonly attemptsRemaining: number
  readonly canRecalibrate: boolean
  /** Final collector counters once browsing collection has stopped. */
  readonly collectorStats: GazeCollectorStats | null
}

export interface ParticipantEyeTrackingIssue {
  readonly stage: 'load' | 'calibration' | 'tracking' | 'preview'
  readonly error: unknown
}

export interface ParticipantEyeTrackingOptions {
  sessionId: string
  transport: EyeTrackingTransport
  maxAttempts?: number
  /** Test seams; the defaults load the real WebGazer build and drive the real camera. */
  loadWebGazer?: () => Promise<LoadedWebGazer>
  createEngine?: (webgazer: LoadedWebGazer) => GazeEngine
  readViewport?: () => Viewport
  environment?: GazeEnvironment
  createBufferPort?: GazeBufferPortFactory
  clock?: SessionClock
  onIssue?: (issue: ParticipantEyeTrackingIssue) => void
}

/**
 * Owns eye tracking for one participant session for the life of the page: the WebGazer engine,
 * the FR-49 calibration controller and, after calibration, the FR-50 gaze collector that takes the
 * same engine over. It lives outside React so re-renders and StrictMode remounts never recreate
 * the camera; React subscribes to immutable snapshots.
 *
 * The participant may always continue: an unavailable camera, a skipped calibration or a poor
 * result all produce a finished attempt, and only `trackingAvailable` decides whether browsing
 * gaze is collected (FR-49). A reload loses the trained model, so a page that starts in BROWSING
 * does not collect gaze.
 */
export class ParticipantEyeTracking {
  readonly sessionId: string
  /** The session time axis. Behavioural collection must share this instance (FR-50). */
  readonly clock: SessionClock
  private readonly options: ParticipantEyeTrackingOptions
  private readonly listeners = new Set<() => void>()
  private stageValue: ParticipantEyeTrackingStage = 'IDLE'
  private controller: CalibrationController | null = null
  private webgazer: LoadedWebGazer | null = null
  private collector: GazeEventCollector | null = null
  private loading: AbortController | null = null
  private target: CalibrationTarget | null = null
  private progress: CalibrationProgress | null = null
  private attempt: CalibrationAttempt | null = null
  private collectorStats: GazeCollectorStats | null = null
  private stopping: Promise<void> | null = null
  private disposedValue = false
  private snapshot: ParticipantEyeTrackingSnapshot

  constructor(options: ParticipantEyeTrackingOptions) {
    if (!options.sessionId.trim()) throw new Error('A participant session ID is required.')
    this.options = { ...options }
    this.sessionId = options.sessionId
    this.clock = options.clock ?? createSessionClock()
    this.snapshot = this.buildSnapshot()
  }

  get stage(): ParticipantEyeTrackingStage {
    return this.stageValue
  }
  get disposed(): boolean {
    return this.disposedValue
  }

  readonly subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener)
    return () => {
      this.listeners.delete(listener)
    }
  }

  readonly getSnapshot = (): ParticipantEyeTrackingSnapshot => this.snapshot

  /**
   * Runs the first attempt, or a recalibration when one is offered. Resolves with the finished
   * attempt; null when nothing was started or the owner was disposed meanwhile.
   */
  async calibrate(): Promise<CalibrationAttempt | null> {
    if (this.disposedValue) return null
    const recalibrating = this.stageValue === 'FINISHED' && this.snapshot.canRecalibrate
    if (this.stageValue !== 'IDLE' && !recalibrating) return null
    this.target = null
    this.progress = null
    this.setStage('STARTING')
    const controller = this.controller ?? (await this.createController())
    if (this.ended()) {
      controller.dispose()
      return null
    }
    const handlers = {
      onTargetChange: (target: CalibrationTarget) => {
        this.target = target
        this.progress = null
        if (this.stageValue === 'STARTING') this.setStage('CALIBRATING')
        else this.publish()
      },
      onProgress: (progress: CalibrationProgress) => {
        this.progress = progress
        this.publish()
      },
    }
    let attempt: CalibrationAttempt
    try {
      attempt = await (controller.attemptsMade === 0
        ? controller.start(handlers)
        : controller.retry(handlers))
    } catch (error) {
      // Only misuse rejects; device and runtime failures resolve with an attempt.
      this.report({ stage: 'calibration', error })
      this.release()
      return null
    }
    // Stopped meanwhile: the camera is already released, so do not offer the result.
    if (this.ended()) return attempt
    this.attempt = attempt
    this.target = null
    this.progress = null
    this.setStage('FINISHED')
    return attempt
  }

  /** Records one click on the current target. The UI draws the target at its own centre. */
  registerClick(): void {
    try {
      this.controller?.registerClick()
    } catch (error) {
      this.report({ stage: 'calibration', error })
    }
  }

  /** Call on resize or zoom while calibrating; a changed viewport ends the attempt. */
  checkViewport(): void {
    this.controller?.checkViewport()
  }

  /**
   * Before the camera is up this ends with UNAVAILABLE/INIT_CANCELLED. While targets are shown it
   * ends the attempt as ABANDONED, keeping the partly trained engine (FR-49).
   */
  skip(): void {
    if (this.stageValue === 'STARTING' && !this.controller) {
      this.loading?.abort()
      return
    }
    this.controller?.skip()
  }

  /**
   * Hands the calibrated engine to a gaze collector for the browsing phase. Returns false, and
   * releases the camera if it is still on, when the latest attempt has no usable engine.
   */
  beginTracking(pageId: string = BROWSING_PAGE_ID): boolean {
    if (this.disposedValue || this.stageValue !== 'FINISHED') return false
    const attempt = this.attempt
    const controller = this.controller
    if (!attempt?.trackingAvailable || !controller) {
      this.release()
      return false
    }
    try {
      const engine = controller.handoffToTracking()
      this.hidePreview()
      const collector = new GazeEventCollector({
        engine,
        sink: { deliver: (batch) => this.options.transport.sendGazeBatch(batch) },
        session: { sessionId: this.sessionId, consentGiven: true, eyeTrackingEnabled: true },
        calibration: attempt,
        clock: this.clock,
        pageId,
        environment: this.options.environment,
        createBufferPort: this.options.createBufferPort,
        drainTimeoutMs: GAZE_DRAIN_TIMEOUT_MS,
        onIssue: (issue) => this.report({ stage: 'tracking', error: issue }),
      })
      this.collector = collector
      collector.start()
    } catch (error) {
      this.report({ stage: 'tracking', error })
      this.release()
      return false
    }
    this.setStage('TRACKING')
    return true
  }

  /**
   * Stops collection and releases the camera. While tracking, resolves once queued gaze has been
   * offered to the server or the drain timeout passed; call it before leaving the browsing phase.
   */
  stopTracking(): Promise<void> {
    if (this.stopping) return this.stopping
    const collector = this.collector
    if (!collector) {
      this.release()
      this.stopping = Promise.resolve()
      return this.stopping
    }
    this.setStage('STOPPING')
    this.stopping = collector
      .stop()
      .catch((error: unknown) => this.report({ stage: 'tracking', error }))
      .then(() => {
        this.collectorStats = collector.stats
        this.controller?.dispose()
        this.setStage('STOPPED')
      })
    return this.stopping
  }

  /** Terminal. Ends any calibration run and stops collection; the camera is released. */
  dispose(): void {
    if (this.disposedValue) return
    this.disposedValue = true
    void this.stopTracking()
  }

  private async createController(): Promise<CalibrationController> {
    const loading = new AbortController()
    this.loading = loading
    let engine: GazeEngine
    try {
      const webgazer = await untilAborted(
        (this.options.loadWebGazer ?? loadWebGazer)(),
        loading.signal,
      )
      engine = this.options.createEngine
        ? this.options.createEngine(webgazer)
        : new WebGazerGazeEngine({
            webgazer,
            showCameraFeedback: true,
            onIssue: (error) => this.report({ stage: 'calibration', error }),
          })
      this.webgazer = webgazer
    } catch (error) {
      const reason: EyeTrackingUnavailableReason = loading.signal.aborted
        ? 'INIT_CANCELLED'
        : 'INIT_FAILED'
      if (reason === 'INIT_FAILED') this.report({ stage: 'load', error })
      // The controller still produces a finished UNAVAILABLE attempt, so the participant can
      // continue and the outcome is recorded like any other.
      engine = unavailableEngine(reason)
    } finally {
      if (this.loading === loading) this.loading = null
    }
    this.controller = new CalibrationController({
      engine,
      session: { sessionId: this.sessionId, consentGiven: true, eyeTrackingEnabled: true },
      sink: { record: (attempt) => this.options.transport.recordCalibrationAttempt(attempt) },
      maxAttempts: this.options.maxAttempts ?? PARTICIPANT_MAX_CALIBRATION_ATTEMPTS,
      targetSettleMs: CALIBRATION_TARGET_SETTLE_MS,
      readViewport: this.options.readViewport,
      onIssue: (issue) => this.report({ stage: 'calibration', error: issue }),
    })
    return this.controller
  }

  /** Releases the camera without collecting anything further. */
  private release(): void {
    this.loading?.abort()
    this.controller?.dispose()
    this.target = null
    this.progress = null
    this.setStage('STOPPED')
  }

  /** WebGazer's preview helps during calibration but must not cover the feed while browsing. */
  private hidePreview(): void {
    const webgazer = this.webgazer
    if (!webgazer) return
    try {
      webgazer.showVideo?.(false)
      webgazer.showFaceOverlay?.(false)
      webgazer.showFaceFeedbackBox?.(false)
      webgazer.showPredictionPoints?.(false)
    } catch (error) {
      this.report({ stage: 'preview', error })
    }
  }

  /** A method, not a field read, because the stage can change while calibrate() awaits. */
  private ended(): boolean {
    return this.disposedValue || this.stageValue === 'STOPPED'
  }

  private setStage(stage: ParticipantEyeTrackingStage): void {
    this.stageValue = stage
    this.publish()
  }

  private publish(): void {
    this.snapshot = this.buildSnapshot()
    for (const listener of [...this.listeners]) {
      try {
        listener()
      } catch {
        /* A subscriber cannot interrupt eye tracking. */
      }
    }
  }

  private buildSnapshot(): ParticipantEyeTrackingSnapshot {
    const attemptsRemaining =
      this.controller?.attemptsRemaining ??
      this.options.maxAttempts ??
      PARTICIPANT_MAX_CALIBRATION_ATTEMPTS
    const attempt = this.attempt
    return Object.freeze({
      stage: this.stageValue,
      target: this.target,
      progress: this.progress,
      attempt,
      attemptsRemaining,
      canRecalibrate:
        this.stageValue === 'FINISHED' &&
        !!attempt?.trackingAvailable &&
        isRetryWorthOffering(attempt.quality) &&
        attemptsRemaining > 0,
      collectorStats: this.collectorStats,
    })
  }

  private report(issue: ParticipantEyeTrackingIssue): void {
    try {
      this.options.onIssue?.(issue)
    } catch {
      /* Diagnostics cannot interrupt the participant. */
    }
  }
}

/** Resolves like `promise`, or rejects as soon as `signal` aborts. */
function untilAborted<T>(promise: Promise<T>, signal: AbortSignal): Promise<T> {
  if (signal.aborted) return Promise.reject(new Error('Cancelled.'))
  return new Promise<T>((resolve, reject) => {
    const onAbort = () => reject(new Error('Cancelled.'))
    signal.addEventListener('abort', onAbort, { once: true })
    promise.then(
      (value) => {
        signal.removeEventListener('abort', onAbort)
        resolve(value)
      },
      (error: unknown) => {
        signal.removeEventListener('abort', onAbort)
        reject(error)
      },
    )
  })
}

/** Stands in when WebGazer itself could not be loaded, so the controller reports the reason. */
function unavailableEngine(reason: EyeTrackingUnavailableReason): GazeEngine {
  return {
    probe: async () => reason,
    start: async () => {
      throw new GazeEngineUnavailableError(reason)
    },
    setSampleListener: () => {},
    recordScreenPosition: () => {
      throw new GazeEngineUnavailableError(reason)
    },
    clearTrainingData: () => {},
    stop: () => {},
  }
}
