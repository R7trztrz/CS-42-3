/** FR-49 public API. See docs/fr-49-calibration.md for ownership and integration contracts. */
export type {
  CalibrationAttempt,
  CalibrationIssue,
  CalibrationOutcome,
  CalibrationPhase,
  CalibrationPointKind,
  CalibrationProgress,
  CalibrationQuality,
  CalibrationSession,
  CalibrationTarget,
  EyeTrackingUnavailableReason,
  LowQualityReason,
  PointResidual,
  QualityThresholds,
  TargetPosition,
  Viewport,
} from './types/calibration'
export type { GazeEngine, GazeSample } from './engine/gazeEngine'
export { GazeEngineUnavailableError } from './engine/gazeEngine'
export type {
  WebGazerLike,
  WebGazerPrediction,
  WebGazerGazeEngineOptions,
} from './engine/webgazerGazeEngine'
export { WebGazerGazeEngine } from './engine/webgazerGazeEngine'
export type {
  CalibrationControllerOptions,
  CalibrationHandlers,
} from './calibration/calibrationController'
export {
  CalibrationController,
  CLICKS_PER_TARGET,
  DEFAULT_MAX_ATTEMPTS,
  NINE_POINT_LAYOUT,
  SAMPLE_FRESHNESS_MS,
} from './calibration/calibrationController'
export type { CalibrationRecordSink } from './calibration/calibrationRecordSink'
export { collectCalibrationAttempts } from './calibration/calibrationRecordSink'
export {
  DEFAULT_QUALITY_THRESHOLDS,
  gradeCalibration,
  isRetryWorthOffering,
} from './calibration/quality'
export {
  buildPointResidual,
  distancePx,
  median,
  medianResidualPx,
  trimmedMean,
} from './calibration/residuals'
