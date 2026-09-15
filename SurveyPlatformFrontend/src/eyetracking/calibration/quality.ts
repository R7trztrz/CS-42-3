import type { CalibrationQuality, QualityThresholds, Viewport } from '../types/calibration'

/** Provisional product thresholds; not a claim of validated physical/angular accuracy. */
export const DEFAULT_QUALITY_THRESHOLDS: QualityThresholds = Object.freeze({
  goodFraction: 0.05,
  fairFraction: 0.1,
})

export function validateViewport(viewport: Viewport): void {
  if (
    [viewport.width, viewport.height, viewport.devicePixelRatio].some(
      (value) => !Number.isFinite(value) || value <= 0,
    )
  ) {
    throw new RangeError('Viewport dimensions and devicePixelRatio must be finite and positive.')
  }
}

export function validateThresholds(thresholds: QualityThresholds): void {
  if (
    !Number.isFinite(thresholds.goodFraction) ||
    !Number.isFinite(thresholds.fairFraction) ||
    thresholds.goodFraction < 0 ||
    thresholds.fairFraction <= thresholds.goodFraction ||
    thresholds.fairFraction > 1
  ) {
    throw new RangeError('Quality thresholds must satisfy 0 <= goodFraction < fairFraction <= 1.')
  }
}

export function gradeCalibration(
  residualMedianPx: number | null,
  viewportWidth: number,
  viewportHeight: number,
  thresholds: QualityThresholds = DEFAULT_QUALITY_THRESHOLDS,
): CalibrationQuality {
  validateThresholds(thresholds)
  if (
    residualMedianPx === null ||
    !Number.isFinite(residualMedianPx) ||
    residualMedianPx < 0 ||
    !Number.isFinite(viewportWidth) ||
    !Number.isFinite(viewportHeight) ||
    viewportWidth <= 0 ||
    viewportHeight <= 0
  )
    return 'NOT_MEASURED'
  const fraction = residualMedianPx / Math.min(viewportWidth, viewportHeight)
  if (fraction <= thresholds.goodFraction) return 'GOOD'
  if (fraction <= thresholds.fairFraction) return 'FAIR'
  return 'POOR'
}

export function isRetryWorthOffering(quality: CalibrationQuality): boolean {
  return quality === 'POOR' || quality === 'NOT_MEASURED'
}
