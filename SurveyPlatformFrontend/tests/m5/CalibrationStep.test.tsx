// @vitest-environment happy-dom

import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'

import type { CalibrationAttempt } from '../../src/eyetracking'
import { ParticipantEyeTracking } from '../../src/eyetracking/participant'
import type { LoadedWebGazer } from '../../src/eyetracking/participant'
import CalibrationStep from '../../src/participant/components/CalibrationStep'
import { FakeEngine } from '../eyetracking/helpers'

const VIEWPORT = { width: 1000, height: 800, devicePixelRatio: 1 }

function owner(engine = new FakeEngine()) {
  const recorded: CalibrationAttempt[] = []
  const eyeTracking = new ParticipantEyeTracking({
    sessionId: 'session-1',
    transport: {
      recordCalibrationAttempt: async (attempt) => {
        recorded.push(attempt)
      },
      sendGazeBatch: async () => {},
    },
    loadWebGazer: async () => ({}) as LoadedWebGazer,
    createEngine: () => engine,
    readViewport: () => VIEWPORT,
  })
  return { eyeTracking, engine, recorded }
}

async function flush(ms = 0) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms)
  })
}

/** Looks at each dot and clicks it once it is enabled, until the overlay closes. */
async function completeCalibration(engine: FakeEngine, offsetPx = 0) {
  for (let guard = 0; guard < 100; guard += 1) {
    const dot = screen.queryByRole('button', { name: 'Calibration point' }) as HTMLButtonElement | null
    if (!dot) return
    await flush(250)
    const x = Number.parseFloat(dot.style.left)
    const y = Number.parseFloat(dot.style.top)
    engine.emit(x, y + offsetPx, performance.now())
    fireEvent.click(dot)
    await flush()
  }
}

describe('CalibrationStep', () => {
  afterEach(() => {
    cleanup()
    vi.useRealTimers()
  })

  it('waits for the eye-tracking owner before offering calibration', () => {
    render(
      <CalibrationStep eyeTracking={null} isCompleting={false} errorMessage={null} onComplete={vi.fn()} />,
    )

    expect(screen.getByRole('status').textContent).toContain('Preparing eye tracking')
  })

  it('runs nine targets on a full-screen overlay and continues with the finished attempt', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'performance', 'Date'] })
    const { eyeTracking, engine, recorded } = owner()
    const onComplete = vi.fn().mockResolvedValue(true)
    render(
      <CalibrationStep
        eyeTracking={eyeTracking}
        isCompleting={false}
        errorMessage={null}
        onComplete={onComplete}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Start calibration' }))
    await flush()
    const dialog = screen.getByRole('dialog', { name: 'Eye-tracking calibration' })
    expect(dialog.parentElement).toBe(document.body)
    const dot = screen.getByRole('button', { name: 'Calibration point' }) as HTMLButtonElement
    // Disabled for the settle window the controller enforces, then live.
    expect(dot.disabled).toBe(true)
    expect(dot.style.left).toBe('120px')
    expect(dot.style.top).toBe('128px')
    await flush(250)
    expect(dot.disabled).toBe(false)
    expect(screen.getByText(/click it 5 more times/)).toBeTruthy()

    await completeCalibration(engine)

    expect(screen.queryByRole('dialog')).toBeNull()
    expect(screen.getByText('Accuracy looks good.')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Recalibrate' })).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Continue' }))
    expect(onComplete).toHaveBeenCalledWith(eyeTracking.getSnapshot().attempt)
    expect(recorded).toEqual([eyeTracking.getSnapshot().attempt])
  })

  it('offers a recalibration only while a poor result can still be improved', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'performance', 'Date'] })
    const { eyeTracking, engine } = owner()
    render(
      <CalibrationStep eyeTracking={eyeTracking} isCompleting={false} errorMessage={null} onComplete={vi.fn()} />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Start calibration' }))
    await flush()
    await completeCalibration(engine, 300)
    expect(screen.getByText('Accuracy is low.')).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: 'Recalibrate' }))
    await flush()
    await completeCalibration(engine, 300)

    expect(eyeTracking.getSnapshot().attempt?.attemptNumber).toBe(2)
    expect(screen.queryByRole('button', { name: 'Recalibrate' })).toBeNull()
  })

  it('lets the participant continue without eye tracking when the camera is unavailable', async () => {
    const engine = new FakeEngine()
    engine.probe.mockResolvedValue('PERMISSION_DENIED')
    const { eyeTracking } = owner(engine)
    const onComplete = vi.fn().mockResolvedValue(true)
    render(
      <CalibrationStep
        eyeTracking={eyeTracking}
        isCompleting={false}
        errorMessage="Something went wrong. Please try again."
        onComplete={onComplete}
      />,
    )

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Start calibration' }))
    })

    expect(screen.getByText('Eye tracking is not available')).toBeTruthy()
    expect(screen.getByText(/Camera access was blocked/)).toBeTruthy()
    expect(screen.getByRole('alert').textContent).toContain('Something went wrong')
    expect(screen.queryByRole('button', { name: 'Recalibrate' })).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Continue' }))
    expect(onComplete).toHaveBeenCalledWith(
      expect.objectContaining({ outcome: 'UNAVAILABLE', unavailableReason: 'PERMISSION_DENIED' }),
    )
  })

  it('records a skipped calibration and keeps the partly trained tracker', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'performance', 'Date'] })
    const { eyeTracking } = owner()
    render(
      <CalibrationStep eyeTracking={eyeTracking} isCompleting={false} errorMessage={null} onComplete={vi.fn()} />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Start calibration' }))
    await flush()
    fireEvent.click(screen.getByRole('button', { name: 'Skip calibration' }))
    await flush()

    expect(screen.getByText('Calibration was not completed')).toBeTruthy()
    expect(eyeTracking.getSnapshot().attempt).toMatchObject({
      outcome: 'ABANDONED',
      trackingAvailable: true,
    })
  })

  it('disables continuing while the completion request is in flight', async () => {
    const engine = new FakeEngine()
    engine.probe.mockResolvedValue('NO_CAMERA_DEVICE')
    const { eyeTracking } = owner(engine)
    const { rerender } = render(
      <CalibrationStep eyeTracking={eyeTracking} isCompleting={false} errorMessage={null} onComplete={vi.fn()} />,
    )
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Start calibration' }))
    })

    rerender(
      <CalibrationStep eyeTracking={eyeTracking} isCompleting errorMessage={null} onComplete={vi.fn()} />,
    )

    expect((screen.getByRole('button', { name: 'Saving...' }) as HTMLButtonElement).disabled).toBe(true)
  })
})
