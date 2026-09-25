import type { EyeTrackingUnavailableReason } from '../types/calibration'
import type { GazeEngine, GazeSample } from './gazeEngine'
import { GazeEngineUnavailableError } from './gazeEngine'

export interface WebGazerPrediction {
  x: number
  y: number
}

/** Supported public WebGazer 3.5.3 surface. The host loads one instance per session. */
export interface WebGazerLike {
  saveDataAcrossSessions(save: boolean): unknown
  setRegression(name: string): unknown
  setGazeListener(listener: (data: WebGazerPrediction | null, elapsedTime: number) => void): unknown
  clearGazeListener(): unknown
  begin(): Promise<unknown>
  end(): unknown
  stopVideo(): unknown
  getRegression(): { init(): unknown }[]
  recordScreenPosition(x: number, y: number, type: string): unknown
  removeMouseEventListeners(): unknown
  applyKalmanFilter?(apply: boolean): unknown
  showVideo?(show: boolean): unknown
  showPredictionPoints?(show: boolean): unknown
  showFaceOverlay?(show: boolean): unknown
  showFaceFeedbackBox?(show: boolean): unknown
  params?: { videoElementId?: string }
}

export interface WebGazerGazeEngineOptions {
  webgazer: WebGazerLike
  regression?: 'ridge'
  showCameraFeedback?: boolean
  now?: () => number
  /** Browser dependencies are injectable for deterministic tests without a real camera. */
  navigator?: {
    mediaDevices?: Pick<MediaDevices, 'getUserMedia' | 'enumerateDevices'>
    permissions?: Pick<Permissions, 'query'>
  }
  document?: Document
  onIssue?: (error: unknown) => void
}

const owners = new WeakSet<WebGazerLike>()

/** Owns camera cleanup, including initialization that completes after cancellation. */
export class WebGazerGazeEngine implements GazeEngine {
  private readonly webgazer: WebGazerLike
  private readonly options: WebGazerGazeEngineOptions
  private readonly now: () => number
  private listener: ((sample: GazeSample) => void) | null = null
  private started = false
  private disposed = false
  private initializing: Promise<void> | null = null
  private observer: MutationObserver | null = null
  private readonly streams = new Set<MediaStream>()
  private began = false
  private claimed = false

  constructor(options: WebGazerGazeEngineOptions) {
    this.options = { ...options }
    this.webgazer = options.webgazer
    this.now = options.now ?? (() => performance.now())
    for (const name of [
      'saveDataAcrossSessions',
      'setRegression',
      'setGazeListener',
      'clearGazeListener',
      'begin',
      'end',
      'stopVideo',
      'getRegression',
      'recordScreenPosition',
      'removeMouseEventListeners',
    ] as const) {
      if (typeof this.webgazer[name] !== 'function')
        throw new TypeError(`WebGazer.${name} is required.`)
    }
  }

  private get document(): Document | undefined {
    return this.options.document ?? globalThis.document
  }

  async probe(): Promise<EyeTrackingUnavailableReason | null> {
    const navigator = this.options.navigator ?? globalThis.navigator
    if (!navigator?.mediaDevices?.getUserMedia) return 'NO_CAMERA_API'
    let permissionGranted = false
    try {
      const permission = await navigator.permissions?.query({ name: 'camera' as PermissionName })
      if (permission?.state === 'denied') return 'PERMISSION_DENIED'
      permissionGranted = permission?.state === 'granted'
    } catch {
      /* Camera permission queries are unsupported in some desktop browsers. */
    }
    try {
      const devices = await navigator.mediaDevices.enumerateDevices()
      // An empty pre-permission list is inconclusive; start() performs the authoritative check.
      if (
        permissionGranted &&
        devices.length > 0 &&
        !devices.some(({ kind }) => kind === 'videoinput')
      )
        return 'NO_CAMERA_DEVICE'
    } catch {
      /* Enumeration is advisory; do not reject a usable camera solely on this failure. */
    }
    return null
  }

  async start(onSample: (sample: GazeSample) => void, signal: AbortSignal): Promise<void> {
    this.assertActive()
    if (signal.aborted) throw new GazeEngineUnavailableError('INIT_CANCELLED')
    this.claim()
    this.listener = onSample
    if (this.started) return
    if (this.initializing) return this.initializing
    const cancel = () => this.stop()
    signal.addEventListener('abort', cancel, { once: true })
    this.initializing = this.initialize(signal).finally(() => {
      this.initializing = null
      signal.removeEventListener('abort', cancel)
      if (this.disposed) this.cleanup()
      this.observer?.disconnect()
      this.observer = null
      if (this.disposed) this.release()
    })
    return this.initializing
  }

  setSampleListener(onSample: ((sample: GazeSample) => void) | null): void {
    if (onSample) this.assertActive()
    this.listener = onSample
  }

  recordScreenPosition(x: number, y: number): void {
    this.assertActive()
    if (!this.started) throw new Error('The gaze engine has not started.')
    this.webgazer.recordScreenPosition(x, y, 'click')
  }

  async clearTrainingData(): Promise<void> {
    this.assertActive()
    this.claim()
    // WebGazer 3.5.3 clearData() clears the origin-wide localforage store as well as
    // regression data. Resetting the public regression instead avoids deleting M5/FR-48 data.
    this.webgazer.saveDataAcrossSessions(false)
    this.webgazer.setRegression(this.options.regression ?? 'ridge')
    // setRegression copies the preceding model's data. The supported regression exposes init().
    const regressions = this.webgazer.getRegression()
    if (
      !regressions?.length ||
      regressions.some((regression) => typeof regression.init !== 'function')
    ) {
      throw new Error(
        'WebGazer.getRegression().init() is required to reset training without clearing shared storage.',
      )
    }
    for (const regression of regressions) await regression.init()
  }

  stop(): void {
    if (this.disposed) return
    this.disposed = true
    this.listener = null
    this.started = false
    if (this.claimed) this.cleanup()
    if (!this.initializing) {
      this.observer?.disconnect()
      this.observer = null
      this.release()
    }
  }

  private async initialize(signal: AbortSignal): Promise<void> {
    try {
      this.webgazer.saveDataAcrossSessions(false)
      this.webgazer.removeMouseEventListeners()
      this.webgazer.setGazeListener((data) => {
        const timestamp = this.now()
        if (
          this.started &&
          !this.disposed &&
          data &&
          Number.isFinite(data.x) &&
          Number.isFinite(data.y) &&
          Number.isFinite(timestamp)
        ) {
          this.safe(() => this.listener?.({ x: data.x, y: data.y, timestamp }))
        }
      })
      const document = this.document
      const Observer = document?.defaultView?.MutationObserver
      if (document?.documentElement && Observer) {
        this.observer = new Observer(() => {
          this.captureStream()
          if (this.disposed) this.cleanup()
          else this.safe(() => this.webgazer.removeMouseEventListeners())
        })
        this.observer.observe(document.documentElement, { childList: true, subtree: true })
      }
      this.began = true
      await this.webgazer.begin()
      this.captureStream()
      if (this.disposed || signal.aborted) throw new GazeEngineUnavailableError('INIT_CANCELLED')
      this.webgazer.removeMouseEventListeners()
      this.webgazer.applyKalmanFilter?.(true)
      this.webgazer.showPredictionPoints?.(false)
      const feedback = this.options.showCameraFeedback ?? true
      this.webgazer.showVideo?.(feedback)
      this.webgazer.showFaceOverlay?.(feedback)
      this.webgazer.showFaceFeedbackBox?.(feedback)
      this.started = true
    } catch (error) {
      this.disposed = true
      this.listener = null
      this.started = false
      this.cleanup()
      throw error instanceof GazeEngineUnavailableError
        ? error
        : new GazeEngineUnavailableError(classifyStartFailure(error), { cause: error })
    }
  }

  private captureStream(): void {
    const element = this.document?.getElementById(
      this.webgazer.params?.videoElementId ?? 'webgazerVideoFeed',
    )
    const stream = (element as HTMLVideoElement | null)?.srcObject as MediaStream | null
    if (stream && typeof stream.getTracks === 'function') this.streams.add(stream)
  }

  private cleanup(): void {
    this.captureStream()
    for (const stream of this.streams) {
      for (const track of stream.getTracks()) this.safe(() => track.stop())
    }
    this.streams.clear()
    this.safe(() => this.webgazer.clearGazeListener())
    this.safe(() => this.webgazer.removeMouseEventListeners())
    if (this.began) {
      // stopVideo handles a stream not yet attached to the DOM. In 3.5.3 end() alone
      // does not stop any tracks. DOM cleanup may throw during partial initialization.
      this.safe(() => this.webgazer.stopVideo())
      this.safe(() => this.webgazer.end())
    }
  }

  private safe(action: () => unknown): void {
    try {
      void Promise.resolve(action()).catch((error: unknown) => this.reportIssue(error))
    } catch (error) {
      this.reportIssue(error)
    }
  }

  private reportIssue(error: unknown): void {
    try {
      void Promise.resolve(this.options.onIssue?.(error)).catch(() => {})
    } catch {
      /* Cleanup must finish even if diagnostics fail. */
    }
  }

  private claim(): void {
    if (this.claimed) return
    if (owners.has(this.webgazer))
      throw new Error('This WebGazer instance already belongs to another session engine.')
    owners.add(this.webgazer)
    this.claimed = true
  }

  private release(): void {
    if (this.claimed) owners.delete(this.webgazer)
    this.claimed = false
  }

  private assertActive(): void {
    if (this.disposed) throw new Error('The gaze engine has been disposed.')
  }
}

function classifyStartFailure(error: unknown): EyeTrackingUnavailableReason {
  const name = error instanceof Error ? error.name : ''
  if (name === 'NotAllowedError' || name === 'SecurityError') return 'PERMISSION_DENIED'
  if (name === 'NotFoundError' || name === 'OverconstrainedError') return 'NO_CAMERA_DEVICE'
  return 'INIT_FAILED'
}
