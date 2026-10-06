// @vitest-environment happy-dom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render } from '@testing-library/react'

import CalibrationStep from '../../src/participant/components/CalibrationStep'

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  cleanup()
  vi.useRealTimers()
})

describe('CalibrationStep', () => {
  it('does not claim completion itself and polls the session on an interval', () => {
    const onRefreshSession = vi.fn().mockResolvedValue(true)
    render(<CalibrationStep onRefreshSession={onRefreshSession} />)

    expect(onRefreshSession).not.toHaveBeenCalled()

    vi.advanceTimersByTime(5000)
    expect(onRefreshSession).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(10000)
    expect(onRefreshSession).toHaveBeenCalledTimes(3)
  })

  it('stops polling after unmount', () => {
    const onRefreshSession = vi.fn().mockResolvedValue(true)
    const { unmount } = render(<CalibrationStep onRefreshSession={onRefreshSession} />)

    unmount()
    vi.advanceTimersByTime(20000)

    expect(onRefreshSession).not.toHaveBeenCalled()
  })
})
