/** FR-50 public API: gaze event collection and widget attribution. */
export type {
  GazeAttribution,
  GazeBufferMode,
  GazeBufferStats,
  GazeCollectorStats,
  GazeEvent,
  GazeEventBatch,
  GazeIssue,
  GazeViewport,
} from './types'
export type {
  SessionClock,
  SessionClockOptions,
  SessionClockReading,
} from './sessionClock'
export { createSessionClock } from './sessionClock'
export type {
  AttributableElement,
  ElementLocator,
  GazeEnvironment,
  WidgetAttributionResult,
} from './widgetAttribution'
export {
  attributeGaze,
  browserGazeEnvironment,
  findWidgetId,
  MAX_ATTRIBUTION_DEPTH,
  NO_CONTENT_ATTRIBUTION,
  UNRESOLVED_ATTRIBUTION,
  WIDGET_ID_ATTRIBUTE,
  WIDGET_SELECTOR,
} from './widgetAttribution'
export type { GazeBatchCollector, GazeEventSink } from './gazeEventSink'
export { collectGazeBatches } from './gazeEventSink'
export type { GazeBufferHost, GazeBufferHostEvents, GazeEventBufferOptions } from './gazeEventBuffer'
export {
  createGazeBufferHost,
  DEFAULT_BATCH_SIZE,
  DEFAULT_MAX_BUFFERED_EVENTS,
  GazeEventBuffer,
} from './gazeEventBuffer'
export type {
  GazeBufferCommand,
  GazeBufferMessage,
  GazeBufferWorkerOptions,
} from './worker/protocol'
export type { GazeBufferPort, GazeBufferPortFactory, GazeBufferPortHandlers } from './worker/workerBuffer'
export {
  createGazeBufferPort,
  createMainThreadBufferPort,
  createWorkerBufferPort,
} from './worker/workerBuffer'
export type { GazeCollectorPhase, GazeEventCollectorOptions } from './gazeEventCollector'
export {
  DEFAULT_DRAIN_TIMEOUT_MS,
  DEFAULT_FLUSH_INTERVAL_MS,
  GazeEventCollector,
} from './gazeEventCollector'
