// @vitest-environment happy-dom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'

import type { CalibrationAttempt } from '../../src/eyetracking'
import { ParticipantEyeTracking } from '../../src/eyetracking/participant'
import type { LoadedWebGazer } from '../../src/eyetracking/participant'
import CalibrationStep from '../../src/participant/components/CalibrationStep'
import { deferred, FakeEngine } from '../eyetracking/helpers'

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

/** A camera stream as getUserMedia returns it, with one video track. */
function fakeStream() {
  const track = {
    kind: 'video',
    readyState: 'live' as MediaStreamTrackState,
    stop: vi.fn(() => {
      track.readyState = 'ended'
    }),
    addEventListener: vi.fn(),
  }
  // A real MediaStream, since a video element accepts nothing else; happy-dom's has no getTracks().
  const stream = new MediaStream([track as unknown as MediaStreamTrack])
  Object.defineProperty(stream, 'getTracks', { value: () => [track] })
  return { stream, track }
}

/** Gives the page a camera: getUserMedia answers with each given stream or error in turn. */
function cameraAnswers(...answers: (MediaStream | Error | Promise<MediaStream>)[]) {
  const getUserMedia = vi.fn()
  for (const answer of answers) {
    getUserMedia.mockImplementationOnce(() =>
      answer instanceof Error ? Promise.reject(answer) : Promise.resolve(answer),
    )
  }
  Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: { getUserMedia } })
  return getUserMedia
}

function cameraError(name: string) {
  return Object.assign(new Error(name), { name })
}

async function flush(ms = 0) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms)
  })
}

async function turnOnCamera() {
  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Turn on camera' }))
  })
}

function preview() {
  return screen.queryByLabelText('Live camera preview') as HTMLVideoElement | null
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
  beforeEach(() => {
    cameraAnswers(fakeStream().stream)
  })

  afterEach(() => {
    cleanup()
    vi.useRealTimers()
    Reflect.deleteProperty(navigator, 'mediaDevices')
    document.getElementById('webgazerVideoFeed')?.remove()
  })

  it('waits for the eye-tracking owner before offering calibration', () => {
    render(
      <CalibrationStep eyeTracking={null} isCompleting={false} errorMessage={null} onComplete={vi.fn()} />,
    )

    expect(screen.getByRole('status').textContent).toContain('Preparing eye tracking')
  })

  it('checks the camera first and closes that stream when calibration starts', async () => {
    const { stream, track } = fakeStream()
    const getUserMedia = cameraAnswers(stream)
    const { eyeTracking } = owner()
    render(
      <CalibrationStep eyeTracking={eyeTracking} isCompleting={false} errorMessage={null} onComplete={vi.fn()} />,
    )

    expect(screen.getByText('Turn on your camera to check your position')).toBeTruthy()
    expect(preview()).toBeNull()
    expect(getUserMedia).not.toHaveBeenCalled()

    await turnOnCamera()
    expect(getUserMedia).toHaveBeenCalledWith({ video: { facingMode: 'user' }, audio: false })
    expect(preview()?.srcObject).toBe(stream)
    // The panel caption; the page's own checklist says the same in a list item.
    expect(screen.getByText('Keep your face inside the guide', { selector: 'p' })).toBeTruthy()
    // The check does not start eye tracking: nothing is loaded until calibration starts.
    expect(eyeTracking.getSnapshot().stage).toBe('IDLE')

    fireEvent.click(screen.getByRole('button', { name: 'Start calibration' }))
    expect(track.stop).toHaveBeenCalledTimes(1)
    expect(eyeTracking.getSnapshot().stage).toBe('STARTING')
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

    await turnOnCamera()
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

  it('shows the camera that stays on in the result panel', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'performance', 'Date'] })
    // The video element WebGazer creates for its camera.
    const webgazerCamera = fakeStream()
    const feed = document.createElement('video')
    feed.id = 'webgazerVideoFeed'
    feed.srcObject = webgazerCamera.stream
    document.body.append(feed)
    const { eyeTracking, engine } = owner()
    render(
      <CalibrationStep eyeTracking={eyeTracking} isCompleting={false} errorMessage={null} onComplete={vi.fn()} />,
    )

    await turnOnCamera()
    fireEvent.click(screen.getByRole('button', { name: 'Start calibration' }))
    await flush()
    await completeCalibration(engine)

    expect(screen.getByText('Accuracy looks good.')).toBeTruthy()
    expect(preview()?.srcObject).toBe(webgazerCamera.stream)
    expect(screen.getByText('Your camera stays on until you finish browsing')).toBeTruthy()
  })

  it('offers a recalibration only while a poor result can still be improved', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'performance', 'Date'] })
    const { eyeTracking, engine } = owner()
    render(
      <CalibrationStep eyeTracking={eyeTracking} isCompleting={false} errorMessage={null} onComplete={vi.fn()} />,
    )

    await turnOnCamera()
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

  it('lets the participant continue without eye tracking when the camera is blocked', async () => {
    cameraAnswers(cameraError('NotAllowedError'))
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

    await turnOnCamera()
    expect(screen.getByText('Camera access was blocked')).toBeTruthy()
    expect(preview()).toBeNull()

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Continue' }))
    })

    expect(screen.getByText('Eye tracking is not available')).toBeTruthy()
    expect(screen.getByText(/Camera access was blocked/)).toBeTruthy()
    expect(screen.getByRole('alert').textContent).toContain('Something went wrong')
    expect(screen.queryByRole('button', { name: 'Recalibrate' })).toBeNull()
    expect(preview()).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Continue' }))
    expect(onComplete).toHaveBeenCalledWith(
      expect.objectContaining({ outcome: 'UNAVAILABLE', unavailableReason: 'PERMISSION_DENIED' }),
    )
  })

  it('lets the participant try the camera again', async () => {
    const { stream } = fakeStream()
    cameraAnswers(cameraError('NotReadableError'), stream)
    const { eyeTracking } = owner()
    render(
      <CalibrationStep eyeTracking={eyeTracking} isCompleting={false} errorMessage={null} onComplete={vi.fn()} />,
    )

    await turnOnCamera()
    expect(screen.getByText(/It may be in use by another app/)).toBeTruthy()

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    })
    expect(preview()?.srcObject).toBe(stream)
    expect(screen.getByRole('button', { name: 'Start calibration' })).toBeTruthy()
  })

  it('closes a camera that answers after the check has gone', async () => {
    const answer = deferred<MediaStream>()
    const { stream, track } = fakeStream()
    cameraAnswers(answer.promise)
    const { eyeTracking } = owner()
    const { unmount } = render(
      <CalibrationStep eyeTracking={eyeTracking} isCompleting={false} errorMessage={null} onComplete={vi.fn()} />,
    )

    await turnOnCamera()
    expect(screen.getByRole('button', { name: 'Starting camera...' })).toBeTruthy()
    unmount()
    await act(async () => {
      answer.resolve(stream)
    })

    expect(track.stop).toHaveBeenCalledTimes(1)
  })

  it('records a skipped calibration and keeps the partly trained tracker', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'performance', 'Date'] })
    const { eyeTracking } = owner()
    render(
      <CalibrationStep eyeTracking={eyeTracking} isCompleting={false} errorMessage={null} onComplete={vi.fn()} />,
    )

    await turnOnCamera()
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
    cameraAnswers(cameraError('NotFoundError'))
    const engine = new FakeEngine()
    engine.probe.mockResolvedValue('NO_CAMERA_DEVICE')
    const { eyeTracking } = owner(engine)
    const { rerender } = render(
      <CalibrationStep eyeTracking={eyeTracking} isCompleting={false} errorMessage={null} onComplete={vi.fn()} />,
    )
    await turnOnCamera()
    expect(screen.getByText('No camera was found')).toBeTruthy()
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Continue' }))
    })

    rerender(
      <CalibrationStep eyeTracking={eyeTracking} isCompleting errorMessage={null} onComplete={vi.fn()} />,
    )

    expect((screen.getByRole('button', { name: 'Saving...' }) as HTMLButtonElement).disabled).toBe(true)
  })
})
