import { describe, expect, it } from 'vitest'
import {
  buildPointResidual,
  gradeCalibration,
  isRetryWorthOffering,
  median,
  trimmedMean,
} from '../../src/eyetracking'

const target = {
  kind: 'CALIBRATION' as const,
  index: 0,
  targetX: 100,
  targetY: 100,
  clicksRequired: 5,
}

describe('Residual arithmetic and quality policy', () => {
  it.each([
    [[10, 20, 30, 40, 1000], 30],
    [[2, 4], 3],
    [[7], 7],
    [[1, 3, 9], 3],
  ])('calculates trimmed mean for %j', (values, expected) => {
    expect(trimmedMean(values)).toBe(expected)
  })
  it('does not mutate samples and averages the two middle values for even medians', () => {
    const values = [7, 1, 5, 3]
    expect(median(values)).toBe(4)
    expect(values).toEqual([7, 1, 5, 3])
  })
  it('preserves the measured centre and computes Euclidean distance', () => {
    const point = buildPointResidual(target, [
      { x: 130, y: 140 },
      { x: 131, y: 139 },
      { x: 129, y: 141 },
    ])
    expect(point).toMatchObject({
      predictedX: 130,
      predictedY: 140,
      residualPx: 50,
      sampleCount: 3,
    })
  })
  it('excludes non-finite observations from counts and never serializes NaN distances', () => {
    expect(buildPointResidual(target, [{ x: NaN, y: 0 }])).toBeNull()
    expect(
      buildPointResidual(target, [
        { x: 100, y: 100 },
        { x: Infinity, y: 0 },
      ])?.sampleCount,
    ).toBe(1)
    expect(buildPointResidual({ ...target, targetX: NaN }, [{ x: 0, y: 0 }])).toBeNull()
    expect(buildPointResidual(target, [])).toBeNull()
    expect(Number.isNaN(trimmedMean([]))).toBe(true)
    expect(Number.isNaN(median([NaN]))).toBe(true)
  })
  it.each([
    [0, 'GOOD'],
    [40, 'GOOD'],
    [40.001, 'FAIR'],
    [80, 'FAIR'],
    [80.001, 'POOR'],
  ] as const)('grades %s px as %s on an 800px short edge', (score, expected) => {
    expect(gradeCalibration(score, 1000, 800)).toBe(expected)
  })
  it.each([null, NaN, Infinity, -1])('does not grade invalid residual %s as GOOD', (score) => {
    expect(gradeCalibration(score, 1000, 800)).toBe('NOT_MEASURED')
  })
  it.each([0, NaN, Infinity, -1])('rejects invalid viewport %s for grading', (dimension) => {
    expect(gradeCalibration(1, dimension, 800)).toBe('NOT_MEASURED')
  })
  it('supports recorded custom thresholds and offers retries only for low/unmeasured results', () => {
    expect(gradeCalibration(30, 1000, 800, { goodFraction: 0.01, fairFraction: 0.02 })).toBe('POOR')
    expect(
      ['GOOD', 'FAIR', 'POOR', 'NOT_MEASURED'].map((quality) =>
        isRetryWorthOffering(quality as 'GOOD' | 'FAIR' | 'POOR' | 'NOT_MEASURED'),
      ),
    ).toEqual([false, false, true, true])
  })
})
