import type { CalibrationTarget, PointResidual } from '../types/calibration'

export interface TargetObservation {
  readonly x: number
  readonly y: number
}

export function trimmedMean(values: readonly number[]): number {
  if (values.length === 0 || values.some((value) => !Number.isFinite(value))) return Number.NaN
  const sorted = [...values].sort((a, b) => a - b)
  const kept = sorted.length >= 3 ? sorted.slice(1, -1) : sorted
  return kept.reduce((sum, value) => sum + value / kept.length, 0)
}

export function median(values: readonly number[]): number {
  if (values.length === 0 || values.some((value) => !Number.isFinite(value))) return Number.NaN
  const sorted = [...values].sort((a, b) => a - b)
  const middle = Math.floor(sorted.length / 2)
  return sorted.length % 2 === 0 ? sorted[middle - 1] / 2 + sorted[middle] / 2 : sorted[middle]
}

export function distancePx(ax: number, ay: number, bx: number, by: number): number {
  return Math.hypot(ax - bx, ay - by)
}

export function buildPointResidual(
  target: CalibrationTarget,
  observations: readonly TargetObservation[],
): PointResidual | null {
  const valid = observations.filter(({ x, y }) => Number.isFinite(x) && Number.isFinite(y))
  if (valid.length === 0) return null
  const predictedX = trimmedMean(valid.map(({ x }) => x))
  const predictedY = trimmedMean(valid.map(({ y }) => y))
  const residualPx = distancePx(target.targetX, target.targetY, predictedX, predictedY)
  if (!Number.isFinite(residualPx)) return null
  return Object.freeze({
    pointKind: target.kind,
    pointIndex: target.index,
    targetX: target.targetX,
    targetY: target.targetY,
    predictedX,
    predictedY,
    residualPx,
    sampleCount: valid.length,
  })
}

export function medianResidualPx(residuals: readonly PointResidual[]): number | null {
  const value = median(residuals.map(({ residualPx }) => residualPx))
  return Number.isFinite(value) ? value : null
}
