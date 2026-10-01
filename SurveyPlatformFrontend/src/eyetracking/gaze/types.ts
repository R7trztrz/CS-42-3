import type { CalibrationQuality, LowQualityReason, Viewport } from '../types/calibration'

/**
 * Why a point carries no widget identity.
 * NO_CONTENT: elementFromPoint returned no element (outside the viewport, or nothing hit there).
 * UNRESOLVED: an element was hit, but no ancestor carries a usable data-widget-id.
 * While the rendering layer has not injected the attribute yet, every hit is UNRESOLVED, which
 * is how a researcher tells a missing-attribute defect from real blank-area gaze.
 */
export type GazeAttribution = 'WIDGET' | 'NO_CONTENT' | 'UNRESOLVED'

/** Viewport plus the scroll offsets needed to recompute attribution after the fact. */
export interface GazeViewport extends Viewport {
  readonly scrollX: number
  readonly scrollY: number
}

/**
 * One prediction, recorded whole. Raw viewport coordinates are never replaced by attribution:
 * a point with no widget is still a complete record.
 */
export interface GazeEvent {
  readonly schemaVersion: 1
  readonly sessionId: string
  readonly pageId: string
  /** Viewport CSS pixels, exactly as predicted. */
  readonly x: number
  readonly y: number
  readonly scrollX: number
  readonly scrollY: number
  /** Document coordinates, so a scrolled page can be replayed without the scroll history. */
  readonly pageX: number
  readonly pageY: number
  readonly viewportWidth: number
  readonly viewportHeight: number
  readonly devicePixelRatio: number
  readonly widgetId: string | null
  readonly attribution: GazeAttribution
  /** Wall clock derived from the session clock; the axis shared with behavioural events. */
  readonly timestamp: string
  /** Monotonic milliseconds since the session clock origin; the ordering key. */
  readonly sessionElapsedMs: number
  readonly calibrationAttemptId: string
  readonly calibrationQuality: CalibrationQuality
  readonly calibrationLowQualityReasons: readonly LowQualityReason[]
}

/**
 * A cut batch. `payload` is already serialized, so the transport owner posts it as-is.
 * `sequence` is gap-free within a session and, with `sessionId`, is a stable idempotency key.
 */
export interface GazeEventBatch {
  readonly schemaVersion: 1
  readonly sessionId: string
  readonly batchId: string
  readonly sequence: number
  readonly eventCount: number
  /** Events discarded by overflow since the previous batch. */
  readonly droppedEvents: number
  /** Events discarded by overflow since the session started; survives a lost batch. */
  readonly droppedEventsTotal: number
  /** JSON array of GazeEvent. Never contains NaN, image data or media objects. */
  readonly payload: string
}

/**
 * A cheap heartbeat from the buffer, emitted on the flush cadence. It exists so overflow is
 * visible while the transport is stuck, rather than only once a later batch gets through.
 */
export interface GazeBufferStats {
  readonly pendingCount: number
  readonly droppedEventsTotal: number
}

export interface GazeIssue {
  readonly stage: 'attribution' | 'viewport' | 'clock' | 'buffer' | 'delivery' | 'engine' | 'drain'
  readonly error: unknown
}

export interface GazeCollectorStats {
  readonly samplesReceived: number
  /** Predictions refused because a coordinate, viewport reading or clock value was not finite. */
  readonly samplesRejected: number
  readonly eventsProduced: number
  /** Events lost to buffer overflow, to a failed worker, or to a batch the sink rejected. */
  readonly eventsDiscarded: number
  /** Events still queued in the buffer, refreshed on the flush cadence. */
  readonly eventsBuffered: number
  readonly batchesDelivered: number
  readonly batchesFailed: number
  readonly attribution: Readonly<Record<GazeAttribution, number>>
}

export type GazeBufferMode = 'WORKER' | 'MAIN_THREAD'
