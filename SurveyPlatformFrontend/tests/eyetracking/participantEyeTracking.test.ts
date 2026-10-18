import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  createMainThreadBufferPort,
  createSessionClock,
  GazeEngineUnavailableError,
} from '../../src/eyetracking'
import type { CalibrationAttempt, GazeEventBatch } from '../../src/eyetracking'
import {
  disposeAllParticipantEyeTracking,
  leaseParticipantEyeTracking,
  ParticipantEyeTracking,
} from '../../src/eyetracking/participant'
import type {
  EyeTrackingTransport,
  LoadedWebGazer,
  ParticipantEyeTrackingOptions,
} from '../../src/eyetracking/participant'
import { deferred, FakeEngine } from './helpers'
import { fakeElement, fakeEnvironment, settle } from './gazeHelpers'

const SESSION_ID = 'session-A'
const VIEWPORT = { width: 1000, height: 800, devicePixelRatio: 2 }

function fakeTransport() {
  const attempts: CalibrationAttempt[] = []
  const batches: GazeEventBatch[] = []
  const transport: EyeTrackingTransport = {
    recordCalibrationAttempt: vi.fn(async (attempt: CalibrationAttempt) => {
      attempts.push(attempt)
    }),
    sendGazeBatch: vi.fn(async (batch: GazeEventBatch) => {
      batches.push(batch)
    }),
  }
  return { transport, attempts, batches }
}

function fakeWebGazer(): LoadedWebGazer {
  return {
    saveDataAcrossSessions: vi.fn(),
    setRegression: vi.fn(),
    setGazeListener: vi.fn(),
    clearGazeListener: vi.fn(),
    begin: vi.fn(async () => undefined),
    end: vi.fn(),
    stopVideo: vi.fn(),
    getRegression: vi.fn(() => []),
    recordScreenPosition: vi.fn(),
    removeMouseEventListeners: vi.fn(),
    showVideo: vi.fn(),
    showFaceOverlay: vi.fn(),
    showFaceFeedbackBox: vi.fn(),
    showPredictionPoints: vi.fn(),
    params: {},
  }
}

function setupOwner(options: Partial<ParticipantEyeTrackingOptions> = {}) {
  const engine = new FakeEngine()
  const webgazer = fakeWebGazer()
  const { transport, attempts, batches } = fakeTransport()
  const world = fakeEnvironment()
  const issues: unknown[] = []
  const owner = new ParticipantEyeTracking({
    sessionId: SESSION_ID,
    transport,
    loadWebGazer: async () => webgazer,
    createEngine: () => engine,
    readViewport: () => VIEWPORT,
    environment: world.environment,
    createBufferPort: createMainThreadBufferPort,
    clock: createSessionClock(),
    onIssue: (issue) => issues.push(issue),
    ...options,
  })
  return { owner, engine, webgazer, transport, attempts, batches, world, issues }
}

/** Looks at each target (a prediction on its centre) and clicks it until the run ends. */
async function lookAndClickThrough(
  owner: ParticipantEyeTracking,
  engine: FakeEngine,
  offset = 0,
) {
  for (let guard = 0; guard < 200 && owner.stage === 'CALIBRATING'; guard += 1) {
    const target = owner.getSnapshot().target
    if (!target) break
    await vi.advanceTimersByTimeAsync(250)
    engine.emit(target.targetX, target.targetY + offset, performance.now())
    owner.registerClick()
  }
  await vi.advanceTimersByTimeAsync(0)
}

async function calibrated(offset = 0, options: Partial<ParticipantEyeTrackingOptions> = {}) {
  const world = setupOwner(options)
  const running = world.owner.calibrate()
  await vi.advanceTimersByTimeAsync(0)
  expect(world.owner.stage).toBe('CALIBRATING')
  await lookAndClickThrough(world.owner, world.engine, offset)
  const attempt = await running
  await vi.advanceTimersByTimeAsync(0)
  return { ...world, attempt: attempt! }
}

describe('ParticipantEyeTracking', () => {
  afterEach(() => {
    disposeAllParticipantEyeTracking()
    vi.useRealTimers()
  })

  it('calibrates once, records the attempt and publishes immutable snapshots', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'performance', 'Date'] })
    const { owner, attempt, attempts } = await calibrated()

    expect(owner.stage).toBe('FINISHED')
    expect(attempt).toMatchObject({
      sessionId: SESSION_ID,
      attemptNumber: 1,
      outcome: 'COMPLETED',
      trackingAvailable: true,
      quality: 'GOOD',
    })
    expect(attempts).toEqual([attempt])
    const snapshot = owner.getSnapshot()
    expect(Object.isFrozen(snapshot)).toBe(true)
    expect(snapshot).toMatchObject({ attempt, canRecalibrate: false, attemptsRemaining: 1 })
    expect(owner.getSnapshot()).toBe(snapshot)
  })

  it('shows the live gaze dot and the preview only while the targets are on screen', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'performance', 'Date'] })
    const { owner, engine, webgazer } = setupOwner()
    const running = owner.calibrate()
    await vi.advanceTimersByTimeAsync(0)

    expect(owner.stage).toBe('CALIBRATING')
    expect(webgazer.showPredictionPoints).toHaveBeenLastCalledWith(true)
    expect(webgazer.showVideo).toHaveBeenLastCalledWith(true)
    expect(owner.getSnapshot()).toMatchObject({ gazeDotVisible: true, canToggleGazeDot: false })

    await lookAndClickThrough(owner, engine)
    await running
    // The result and set-up screens that follow show neither, although the camera stays on.
    expect(owner.getSnapshot()).toMatchObject({ stage: 'FINISHED', gazeDotVisible: false })
    expect(webgazer.showPredictionPoints).toHaveBeenLastCalledWith(false)
    expect(webgazer.showVideo).toHaveBeenLastCalledWith(false)
    expect(webgazer.showFaceOverlay).toHaveBeenLastCalledWith(false)
    expect(webgazer.showFaceFeedbackBox).toHaveBeenLastCalledWith(false)
  })

  it('shows the preview again for a recalibration', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'performance', 'Date'] })
    const { owner, engine, webgazer } = await calibrated(300)
    expect(webgazer.showVideo).toHaveBeenLastCalledWith(false)

    const retry = owner.calibrate()
    await vi.advanceTimersByTimeAsync(0)
    expect(owner.stage).toBe('CALIBRATING')
    expect(webgazer.showVideo).toHaveBeenLastCalledWith(true)
    expect(owner.getSnapshot().gazeDotVisible).toBe(true)

    await lookAndClickThrough(owner, engine, 300)
    await retry
    expect(webgazer.showVideo).toHaveBeenLastCalledWith(false)
  })

  it('gives the page the live camera stream WebGazer is using, and none once it stops', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'performance', 'Date'] })
    // WebGazer's own video element, which holds its camera stream.
    const track = { readyState: 'live' }
    const stream = { getVideoTracks: () => [track] } as unknown as MediaStream
    const feed = { srcObject: stream }
    vi.stubGlobal('document', {
      getElementById: (id: string) => (id === 'webgazerVideoFeed' ? feed : null),
    })
    try {
      // Nothing is loaded before calibration starts.
      expect(setupOwner().owner.cameraStream()).toBeNull()

      const { owner } = await calibrated()
      expect(owner.cameraStream()).toBe(stream)

      track.readyState = 'ended'
      expect(owner.cameraStream()).toBeNull()

      track.readyState = 'live'
      owner.dispose()
      expect(owner.cameraStream()).toBeNull()
    } finally {
      vi.unstubAllGlobals()
    }
  })

  it('offers one recalibration after a poor result and then no more', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'performance', 'Date'] })
    const { owner, engine, attempt } = await calibrated(300)
    expect(attempt.quality).toBe('POOR')
    expect(owner.getSnapshot().canRecalibrate).toBe(true)

    const retry = owner.calibrate()
    await vi.advanceTimersByTimeAsync(0)
    await lookAndClickThrough(owner, engine, 300)
    const second = await retry

    expect(second).toMatchObject({ attemptNumber: 2, quality: 'POOR' })
    expect(owner.getSnapshot()).toMatchObject({ canRecalibrate: false, attemptsRemaining: 0 })
    expect(await owner.calibrate()).toBeNull()
  })

  it('finishes with an UNAVAILABLE attempt when WebGazer cannot be loaded', async () => {
    const { owner, issues, attempts } = setupOwner({
      loadWebGazer: async () => {
        throw new Error('network down')
      },
    })

    const attempt = await owner.calibrate()

    expect(attempt).toMatchObject({
      outcome: 'UNAVAILABLE',
      trackingAvailable: false,
      unavailableReason: 'INIT_FAILED',
    })
    expect(owner.getSnapshot()).toMatchObject({ stage: 'FINISHED', canRecalibrate: false })
    expect(issues).toContainEqual(expect.objectContaining({ stage: 'load' }))
    expect(attempts).toEqual([attempt])
  })

  it('cancels a slow load when the participant skips', async () => {
    const load = deferred<LoadedWebGazer>()
    const { owner } = setupOwner({ loadWebGazer: () => load.promise })

    const running = owner.calibrate()
    expect(owner.stage).toBe('STARTING')
    owner.skip()

    expect(await running).toMatchObject({
      outcome: 'UNAVAILABLE',
      unavailableReason: 'INIT_CANCELLED',
    })
  })

  it('reports a camera the engine cannot start as unavailable', async () => {
    const engine = new FakeEngine()
    engine.start.mockRejectedValue(new GazeEngineUnavailableError('PERMISSION_DENIED'))
    const { owner } = setupOwner({ createEngine: () => engine })

    expect(await owner.calibrate()).toMatchObject({
      outcome: 'UNAVAILABLE',
      unavailableReason: 'PERMISSION_DENIED',
    })
    expect(owner.beginTracking()).toBe(false)
    expect(owner.stage).toBe('STOPPED')
  })

  it('hands the calibrated engine to gaze collection, hides the preview and drains on stop', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'performance', 'Date'] })
    const { owner, engine, webgazer, batches, world, attempt } = await calibrated()
    world.setHit(() => fakeElement('node-1'))

    expect(owner.beginTracking()).toBe(true)
    expect(owner.stage).toBe('TRACKING')
    expect(webgazer.showVideo).toHaveBeenCalledWith(false)
    expect(webgazer.showFaceFeedbackBox).toHaveBeenCalledWith(false)
    // Real participants: no gaze dot while browsing, and no switch to turn it on.
    expect(webgazer.showPredictionPoints).toHaveBeenLastCalledWith(false)
    expect(owner.getSnapshot()).toMatchObject({ gazeDotVisible: false, canToggleGazeDot: false })
    owner.setGazeDotVisible(true)
    expect(webgazer.showPredictionPoints).toHaveBeenLastCalledWith(false)
    expect(owner.beginTracking()).toBe(false)

    engine.emit(10, 20, performance.now())
    engine.emit(30, 40, performance.now())
    const stopping = owner.stopTracking()
    expect(owner.stage).toBe('STOPPING')
    await vi.advanceTimersByTimeAsync(0)
    await settle()
    await stopping

    expect(owner.stage).toBe('STOPPED')
    expect(engine.stop).toHaveBeenCalled()
    expect(batches).toHaveLength(1)
    const events = JSON.parse(batches[0].payload) as { widgetId: string; calibrationAttemptId: string }[]
    expect(events).toHaveLength(2)
    expect(events[0]).toMatchObject({ widgetId: 'node-1', calibrationAttemptId: attempt.attemptId })
    expect(owner.getSnapshot().collectorStats).toMatchObject({ eventsProduced: 2, batchesDelivered: 1 })
    expect(owner.stopTracking()).toBe(owner.stopTracking())
  })

  it('in test mode shows the gaze dot while browsing and lets it be switched off and on', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'performance', 'Date'] })
    const { owner, webgazer } = await calibrated(0, { showGazeDotWhileBrowsing: true })

    expect(owner.beginTracking()).toBe(true)
    expect(webgazer.showPredictionPoints).toHaveBeenLastCalledWith(true)
    expect(owner.getSnapshot()).toMatchObject({ gazeDotVisible: true, canToggleGazeDot: true })

    owner.setGazeDotVisible(false)
    expect(webgazer.showPredictionPoints).toHaveBeenLastCalledWith(false)
    expect(owner.getSnapshot().gazeDotVisible).toBe(false)
    owner.setGazeDotVisible(true)
    expect(webgazer.showPredictionPoints).toHaveBeenLastCalledWith(true)
    expect(owner.getSnapshot().gazeDotVisible).toBe(true)

    const stopping = owner.stopTracking()
    await vi.advanceTimersByTimeAsync(0)
    await settle()
    await stopping
    expect(webgazer.showPredictionPoints).toHaveBeenLastCalledWith(false)
    expect(owner.getSnapshot()).toMatchObject({ gazeDotVisible: false, canToggleGazeDot: false })
  })

  it('does not collect on a page that never calibrated', async () => {
    const { owner } = setupOwner()

    expect(owner.beginTracking()).toBe(false)
    await owner.stopTracking()

    expect(owner.stage).toBe('STOPPED')
    expect(owner.getSnapshot().collectorStats).toBeNull()
  })

  it('disposing mid-calibration ends the run and releases the camera', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'performance', 'Date'] })
    const { owner, engine, attempts } = setupOwner()
    const running = owner.calibrate()
    await vi.advanceTimersByTimeAsync(0)
    expect(owner.stage).toBe('CALIBRATING')

    owner.dispose()

    expect(await running).toMatchObject({ outcome: 'ABANDONED', unavailableReason: 'INIT_CANCELLED' })
    await vi.advanceTimersByTimeAsync(0)
    expect(owner.stage).toBe('STOPPED')
    expect(owner.disposed).toBe(true)
    expect(engine.stop).toHaveBeenCalled()
    expect(attempts).toHaveLength(1)
  })
})

describe('leaseParticipantEyeTracking', () => {
  afterEach(() => {
    disposeAllParticipantEyeTracking()
    vi.useRealTimers()
  })

  it('keeps one owner across an immediate release and re-lease (StrictMode remount)', async () => {
    vi.useFakeTimers()
    const create = vi.fn(() => setupOwner().owner)

    const first = leaseParticipantEyeTracking(SESSION_ID, create)
    first.release()
    const second = leaseParticipantEyeTracking(SESSION_ID, create)
    await vi.runAllTimersAsync()

    expect(create).toHaveBeenCalledTimes(1)
    expect(second.owner).toBe(first.owner)
    expect(second.owner.disposed).toBe(false)

    second.release()
    second.release()
    await vi.runAllTimersAsync()
    expect(second.owner.disposed).toBe(true)
    expect(leaseParticipantEyeTracking(SESSION_ID, create).owner).not.toBe(first.owner)
  })

  it('disposes the previous session when another one leases the page-wide engine', () => {
    const first = leaseParticipantEyeTracking('session-1', () => setupOwner().owner)
    const second = leaseParticipantEyeTracking('session-2', () =>
      setupOwner({ sessionId: 'session-2' }).owner,
    )

    expect(first.owner.disposed).toBe(true)
    expect(second.owner.disposed).toBe(false)
  })
})
