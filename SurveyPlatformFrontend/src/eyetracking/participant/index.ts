/**
 * M6 participant integration: loads WebGazer, runs FR-49 calibration and FR-50 browsing
 * collection for one participant session, and delivers both to the backend.
 */
export type {
  DeliveryRetryPolicy,
  EyeTrackingTransport,
} from './eyeTrackingTransport'
export {
  CALIBRATION_ATTEMPTS_PATH,
  createHttpEyeTrackingTransport,
  DEFAULT_DELIVERY_RETRY_POLICY,
  deliverWithRetries,
  GAZE_BATCHES_PATH,
  isRetryableDeliveryError,
} from './eyeTrackingTransport'
export type { ParticipantEyeTrackingLease } from './ownerRegistry'
export { disposeAllParticipantEyeTracking, leaseParticipantEyeTracking } from './ownerRegistry'
export type {
  ParticipantEyeTrackingIssue,
  ParticipantEyeTrackingOptions,
  ParticipantEyeTrackingSnapshot,
  ParticipantEyeTrackingStage,
} from './participantEyeTracking'
export {
  BROWSING_PAGE_ID,
  CALIBRATION_TARGET_SETTLE_MS,
  GAZE_DRAIN_TIMEOUT_MS,
  PARTICIPANT_MAX_CALIBRATION_ATTEMPTS,
  ParticipantEyeTracking,
} from './participantEyeTracking'
export type { LoadedWebGazer, LoadWebGazerOptions, WebGazerAssets } from './webgazerLoader'
export {
  DEFAULT_WEBGAZER_LOAD_TIMEOUT_MS,
  defaultWebGazerAssets,
  loadWebGazer,
} from './webgazerLoader'
