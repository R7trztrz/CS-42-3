/** FR-49 metadata. Browser predictions are never emitted as gaze events by this module. */
export type EyeTrackingUnavailableReason =
  | 'NO_CAMERA_API'
  | 'NO_CAMERA_DEVICE'
  | 'PERMISSION_DENIED'
  | 'INIT_FAILED'
  | 'INIT_CANCELLED'
  | 'INIT_TIMEOUT'

export type CalibrationOutcome = 'COMPLETED' | 'ABANDONED' | 'UNAVAILABLE'
export type CalibrationQuality = 'GOOD' | 'FAIR' | 'POOR' | 'NOT_MEASURED'
export type CalibrationPointKind = 'CALIBRATION' | 'VALIDATION'
export type CalibrationPhase =
  | 'IDLE'
  | 'STARTING'
  | 'RUNNING'
  | 'FINISHED'
  | 'HANDED_OFF'
  | 'DISPOSED'
export type LowQualityReason =
  | 'ABANDONED'
  | 'NOT_MEASURED'
  | 'INSUFFICIENT_SAMPLES'
  | 'HIGH_RESIDUAL'
  | 'VIEWPORT_CHANGED'
  | 'ENGINE_UNAVAILABLE'

export interface Viewport {
  readonly width: number
  readonly height: number
  readonly devicePixelRatio: number
}

/** M5 supplies this context after consent. On revocation, the current engine owner must stop it. */
export interface CalibrationSession {
  readonly sessionId: string
  readonly consentGiven: boolean
  readonly eyeTrackingEnabled: boolean
}

export interface TargetPosition {
  readonly xFraction: number
  readonly yFraction: number
}

export interface CalibrationTarget {
  readonly kind: CalibrationPointKind
  readonly index: number
  /** Target centre in viewport CSS pixels, not page or physical display pixels. */
  readonly targetX: number
  readonly targetY: number
  readonly clicksRequired: number
}

export interface CalibrationProgress {
  readonly target: CalibrationTarget
  readonly clicksRecorded: number
  readonly validSamples: number
  /** Includes the current target once its final click has been recorded. */
  readonly targetsCompleted: number
  readonly targetsTotal: number
}

export interface PointResidual {
  readonly pointKind: CalibrationPointKind
  readonly pointIndex: number
  readonly targetX: number
  readonly targetY: number
  readonly predictedX: number
  readonly predictedY: number
  readonly residualPx: number
  /** Distinct valid predictions, before coordinate-wise trimming. */
  readonly sampleCount: number
}

export interface QualityThresholds {
  readonly goodFraction: number
  readonly fairFraction: number
}

/** An immutable, session-bound handoff to FR-48/FR-51; it is not a transport schema. */
export interface CalibrationAttempt {
  readonly schemaVersion: 1
  readonly algorithmVersion: 'residual-v2'
  readonly sessionId: string
  readonly attemptId: string
  readonly attemptNumber: number
  readonly outcome: CalibrationOutcome
  readonly trackingAvailable: boolean
  /** Can also describe an engine failure during an abandoned run. */
  readonly unavailableReason: EyeTrackingUnavailableReason | null
  readonly quality: CalibrationQuality
  readonly qualityBasis: CalibrationPointKind | 'NONE'
  readonly lowQualityReasons: readonly LowQualityReason[]
  readonly qualityThresholds: QualityThresholds
  readonly minSamplesPerTarget: number
  readonly calibrationTargetsMeasured: number
  readonly calibrationTargetsRequired: number
  readonly validationTargetsMeasured: number
  readonly validationTargetsRequired: number
  readonly residualMedianPx: number | null
  readonly validationMedianPx: number | null
  readonly viewportWidth: number
  readonly viewportHeight: number
  readonly devicePixelRatio: number
  readonly startedAt: string
  readonly finishedAt: string
  readonly pointResiduals: readonly PointResidual[]
}

export interface CalibrationIssue {
  readonly stage: 'handler' | 'recording' | 'engine'
  readonly error: unknown
  readonly attemptId?: string
}
