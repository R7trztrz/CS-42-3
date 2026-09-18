import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  CalibrationController,
  GazeEngineUnavailableError,
  collectCalibrationAttempts,
} from '../../src/eyetracking'
import { deferred, FakeEngine, flush, setup } from './helpers'

afterEach(() => vi.useRealTimers())

describe('FR-49 preconditions and configuration', () => {
  it.each([
    [false, true],
    [true, false],
    [false, false],
  ])(
    'does not touch the engine without consent=%s / enabled=%s',
    async (consentGiven, eyeTrackingEnabled) => {
      const { controller, engine } = setup({
        session: { sessionId: 'A', consentGiven, eyeTrackingEnabled },
      })
      await expect(controller.start()).rejects.toThrow('requires consent')
      await expect(controller.probeAvailability()).rejects.toThrow('requires consent')
      expect(engine.probe).not.toHaveBeenCalled()
      expect(engine.start).not.toHaveBeenCalled()
      expect(engine.clearTrainingData).not.toHaveBeenCalled()
      expect(controller.attempts).toEqual([])
    },
  )
  it.each([0, -1, NaN, Infinity, 1.5, 11])('rejects invalid maxAttempts %s', (maxAttempts) => {
    expect(() => setup({ maxAttempts })).toThrow(RangeError)
  })
  it('rejects malformed context, thresholds and layouts', () => {
    expect(() =>
      setup({ session: { sessionId: ' ', consentGiven: true, eyeTrackingEnabled: true } }),
    ).toThrow()
    expect(() => setup({ qualityThresholds: { goodFraction: 0.2, fairFraction: 0.1 } })).toThrow()
    expect(() => setup({ validationLayout: [{ xFraction: NaN, yFraction: 0.1 }] })).toThrow()
    expect(() => setup({ minSamplesPerTarget: 6 })).toThrow()
    expect(() => setup({ startupTimeoutMs: Infinity })).toThrow()
  })
  it('validates viewport before opening the camera or consuming an attempt', async () => {
    const { controller, engine } = setup({
      readViewport: () => ({ width: 0, height: 800, devicePixelRatio: 1 }),
    })
    await expect(controller.start()).rejects.toThrow(RangeError)
    expect(engine.start).not.toHaveBeenCalled()
    expect(controller.attemptsMade).toBe(0)
  })
  it('restores the attempt count without reusing earlier ordinals', async () => {
    const { controller, complete } = setup({ previousAttempts: 1 })
    const result = controller.start()
    await flush()
    complete()
    expect((await result).attemptNumber).toBe(2)
    await expect(controller.retry()).rejects.toThrow('limit')
  })
})

describe('UC-35 completed calibration and quality', () => {
  it('trains 9 x 5 positions and produces a session-bound immutable quality record', async () => {
    const { controller, engine, complete } = setup()
    const progress = vi.fn()
    const result = controller.start({ onProgress: progress })
    await flush()
    complete(30)
    const record = await result
    expect(record).toMatchObject({
      sessionId: 'session-A',
      attemptId: 'attempt-1',
      attemptNumber: 1,
      outcome: 'COMPLETED',
      quality: 'GOOD',
      trackingAvailable: true,
      calibrationTargetsMeasured: 9,
      residualMedianPx: 30,
      qualityBasis: 'CALIBRATION',
    })
    expect(record.pointResiduals).toHaveLength(9)
    expect(record.pointResiduals.every((point) => point.sampleCount === 5)).toBe(true)
    expect(engine.recordScreenPosition).toHaveBeenCalledTimes(45)
    expect(progress.mock.lastCall?.[0].targetsCompleted).toBe(9)
    expect(Object.isFrozen(record)).toBe(true)
    expect(Object.isFrozen(record.pointResiduals)).toBe(true)
    expect(Object.isFrozen(record.pointResiduals[0])).toBe(true)
    expect(controller.pendingRecords).toEqual([record])
    expect(engine.stop).not.toHaveBeenCalled()
  })
  it('labels an accurately measured but abandoned attempt as low quality', async () => {
    const { controller, click } = setup()
    const result = controller.start()
    await flush()
    click()
    controller.skip()
    expect(await result).toMatchObject({
      outcome: 'ABANDONED',
      quality: 'POOR',
      trackingAvailable: true,
    })
    expect(controller.currentAttempt?.lowQualityReasons).toContain('ABANDONED')
  })
  it('distinguishes an unmeasured run from a measured poor run without stopping tracking', async () => {
    const { controller, engine } = setup()
    const result = controller.start()
    await flush()
    while (controller.running) controller.registerClick()
    expect(await result).toMatchObject({
      outcome: 'COMPLETED',
      quality: 'NOT_MEASURED',
      residualMedianPx: null,
      trackingAvailable: true,
    })
    expect(engine.stop).not.toHaveBeenCalled()
  })
  it('does not give GOOD to one measured target and eight empty targets', async () => {
    const { controller, click } = setup()
    const result = controller.start()
    await flush()
    click()
    while (controller.running) controller.registerClick()
    const record = await result
    expect(record.quality).toBe('POOR')
    expect(record.lowQualityReasons).toContain('INSUFFICIENT_SAMPLES')
  })
  it('uses held-out validation when requested, without feeding validation into training', async () => {
    const { controller, engine, click } = setup({
      validationLayout: [{ xFraction: 0.3, yFraction: 0.7 }],
    })
    const result = controller.start()
    await flush()
    while (controller.running) click(controller.currentTarget!.kind === 'VALIDATION' ? 400 : 0)
    expect(await result).toMatchObject({
      quality: 'POOR',
      qualityBasis: 'VALIDATION',
      residualMedianPx: 0,
      validationMedianPx: 400,
    })
    expect(engine.recordScreenPosition).toHaveBeenCalledTimes(45)
  })
  it('will not fall back to a good training score when validation was requested but never measured', async () => {
    const { controller, click } = setup({ validationLayout: [{ xFraction: 0.3, yFraction: 0.7 }] })
    const result = controller.start()
    await flush()
    while (controller.currentTarget?.kind === 'CALIBRATION') click()
    while (controller.running) controller.registerClick()
    expect(await result).toMatchObject({ quality: 'NOT_MEASURED', qualityBasis: 'NONE' })
  })
  it('keeps finish time monotonic when wall time moves backwards', async () => {
    const { controller, complete, clock } = setup()
    const result = controller.start()
    await flush()
    clock.wall = 0
    complete()
    const record = await result
    expect(Date.parse(record.finishedAt) - Date.parse(record.startedAt)).toBe(clock.time)
  })
  it('limits retries and replaces training while keeping all records', async () => {
    const sink = collectCalibrationAttempts('session-A')
    const { controller, engine, complete } = setup({ sink })
    const first = controller.start()
    await flush()
    complete(400)
    await first
    const second = controller.retry()
    await flush()
    complete(0)
    await second
    await flush()
    expect(engine.clearTrainingData).toHaveBeenCalledTimes(2)
    expect(sink.attempts).toHaveLength(2)
    expect(sink.current?.attemptNumber).toBe(2)
    expect(controller.pendingRecords).toHaveLength(0)
    await expect(controller.retry()).rejects.toThrow('limit')
  })
})

describe('Sample identity, target coordinates and viewport', () => {
  it('uses the actual displayed centre for both training and residuals', async () => {
    const { controller, engine } = setup()
    const result = controller.start()
    await flush()
    const target = controller.currentTarget!
    engine.emit(target.targetX + 30, target.targetY + 40, 0)
    controller.registerClick(target.targetX + 30, target.targetY + 40)
    controller.skip()
    const point = (await result).pointResiduals[0]
    expect(point.residualPx).toBe(0)
    expect(engine.recordScreenPosition).toHaveBeenCalledWith(point.targetX, point.targetY)
  })
  it('consumes each callback once and never carries it into another target', async () => {
    const { controller, engine } = setup()
    const result = controller.start()
    await flush()
    const target = controller.currentTarget!
    engine.emit(target.targetX, target.targetY, 0)
    for (let i = 0; i < 6; i += 1) controller.registerClick()
    controller.skip()
    const record = await result
    expect(record.pointResiduals).toHaveLength(1)
    expect(record.pointResiduals[0].sampleCount).toBe(1)
  })
  it.each([-701, -700, 1, NaN])(
    'ignores stale, future and invalid sample time %s',
    async (timestamp) => {
      const { controller, engine } = setup()
      const result = controller.start()
      await flush()
      engine.emit(120, 128, timestamp)
      controller.registerClick()
      controller.skip()
      expect((await result).pointResiduals).toHaveLength(0)
    },
  )
  it('accepts a sample just within the freshness window', async () => {
    const { controller, engine, clock } = setup()
    const result = controller.start()
    await flush()
    engine.emit(120, 128, 0)
    clock.time = 699
    controller.registerClick()
    controller.skip()
    expect((await result).pointResiduals[0].sampleCount).toBe(1)
  })
  it('does not train or advance during target settling', async () => {
    const { controller, engine, clock } = setup({ targetSettleMs: 200 })
    const result = controller.start()
    await flush()
    engine.emit(120, 128, 0)
    controller.registerClick()
    expect(engine.recordScreenPosition).not.toHaveBeenCalled()
    clock.time = 200
    engine.emit(120, 128, 200)
    controller.registerClick()
    controller.skip()
    expect((await result).pointResiduals[0].sampleCount).toBe(1)
  })
  it('rejects an out-of-bounds centre without corrupting the run', async () => {
    const { controller } = setup()
    const result = controller.start()
    await flush()
    expect(() => controller.registerClick(-1, 0)).toThrow(RangeError)
    controller.skip()
    await result
  })
  it('finishes as low quality if the viewport changes', async () => {
    const { controller, click, viewport, engine } = setup()
    const result = controller.start()
    await flush()
    click()
    viewport.width = 1200
    expect(controller.checkViewport()).toBe(false)
    expect((await result).lowQualityReasons).toContain('VIEWPORT_CHANGED')
    expect(engine.stop).not.toHaveBeenCalled()
  })
  it('does not mix two measured centres within one target', async () => {
    const { controller, click } = setup()
    const result = controller.start()
    await flush()
    click()
    controller.registerClick(200, 200)
    expect((await result).lowQualityReasons).toContain('VIEWPORT_CHANGED')
  })
})

describe('Startup, cancellation and ownership regressions', () => {
  it('rejects a second start before the first engine start resolves', async () => {
    const { controller, engine } = setup()
    const gate = deferred<void>()
    engine.startGate = gate.promise
    const result = controller.start()
    await flush()
    await expect(controller.start()).rejects.toThrow('already')
    gate.resolve()
    await flush()
    controller.skip()
    await result
    expect(engine.start).toHaveBeenCalledTimes(1)
  })
  it.each(['skip', 'dispose'] as const)(
    'settles %s during initialization and stops a late engine',
    async (action) => {
      const { controller, engine } = setup()
      const gate = deferred<void>()
      engine.startGate = gate.promise
      const result = controller.start()
      await flush()
      controller[action]()
      expect(await result).toMatchObject({
        trackingAvailable: false,
        unavailableReason: 'INIT_CANCELLED',
      })
      gate.resolve()
      await flush()
      expect(controller.running).toBe(false)
      expect(engine.stop).toHaveBeenCalled()
      expect(controller.attempts).toHaveLength(1)
    },
  )
  it('settles dispose while running and prohibits reuse', async () => {
    const { controller } = setup()
    const result = controller.start()
    await flush()
    controller.dispose()
    expect((await result).outcome).toBe('ABANDONED')
    expect(controller.phase).toBe('DISPOSED')
    await expect(controller.start()).rejects.toThrow('no longer owns')
  })
  it('settles a synchronous skip from the first target callback', async () => {
    const { controller } = setup()
    expect((await controller.start({ onTargetChange: () => controller.skip() })).outcome).toBe(
      'ABANDONED',
    )
  })
  it('allows a synchronous skip from progress without advancing a finished run', async () => {
    const { controller, click } = setup()
    const result = controller.start({ onProgress: () => controller.skip() })
    await flush()
    click()
    expect((await result).outcome).toBe('ABANDONED')
    expect(controller.currentTarget).toBeNull()
  })
  it('bounds initialization even if the engine never resolves', async () => {
    vi.useFakeTimers()
    const { controller, engine } = setup({ startupTimeoutMs: 100 })
    engine.startGate = new Promise(() => {})
    const result = controller.start()
    await flush()
    await vi.advanceTimersByTimeAsync(100)
    expect(await result).toMatchObject({
      outcome: 'UNAVAILABLE',
      unavailableReason: 'INIT_TIMEOUT',
    })
    expect(engine.stop).toHaveBeenCalled()
  })
  it.each(['sync', 'async'])(
    'turns %s training reset failure into a recorded unavailable outcome',
    async (kind) => {
      const { controller, engine } = setup()
      if (kind === 'sync')
        engine.clearTrainingData.mockImplementation(() => {
          throw new Error('reset')
        })
      else engine.clearTrainingData.mockRejectedValue(new Error('reset'))
      expect(await controller.start()).toMatchObject({
        outcome: 'UNAVAILABLE',
        unavailableReason: 'INIT_FAILED',
      })
      expect(controller.pendingRecords).toHaveLength(1)
    },
  )
  it('records permission denial and prevents tracking handoff', async () => {
    const { controller, engine } = setup()
    engine.start.mockRejectedValue(new GazeEngineUnavailableError('PERMISSION_DENIED'))
    expect(await controller.start()).toMatchObject({
      outcome: 'UNAVAILABLE',
      unavailableReason: 'PERMISSION_DENIED',
    })
    expect(() => controller.handoffToTracking()).toThrow()
  })
  it('handles an engine failure on a training click without blocking the result', async () => {
    const { controller, engine, click } = setup()
    const result = controller.start()
    await flush()
    engine.recordScreenPosition.mockImplementation(() => {
      throw new Error('model failed')
    })
    click()
    expect(await result).toMatchObject({
      outcome: 'ABANDONED',
      trackingAvailable: false,
      unavailableReason: 'INIT_FAILED',
    })
  })
  it('hands off the same poor-quality model and prevents page disposal from stopping FR-50', async () => {
    const { controller, engine, complete } = setup()
    expect(() => controller.handoffToTracking()).toThrow()
    const result = controller.start()
    await flush()
    complete(400)
    await result
    expect(controller.handoffToTracking()).toBe(engine)
    const consume = vi.fn()
    engine.setSampleListener(consume)
    controller.dispose()
    engine.emit(1, 2, 100)
    expect(consume).toHaveBeenCalledOnce()
    expect(engine.stop).not.toHaveBeenCalled()
    await expect(controller.retry()).rejects.toThrow('no longer owns')
  })
})

describe('Reliable record handoff and callback isolation', () => {
  it.each(['sync', 'async'])(
    'retains a record after %s sink failure and retries the same ID',
    async (kind) => {
      const record = vi.fn<(attempt: unknown) => void | Promise<void>>(() => {
        if (kind === 'sync') throw new Error('queue failed')
        return Promise.reject(new Error('queue failed'))
      })
      const { controller, complete } = setup({ sink: { record } })
      const result = controller.start()
      await flush()
      complete()
      const attempt = await result
      await flush()
      expect(controller.pendingRecords).toEqual([attempt])
      expect(controller.issues.at(-1)?.stage).toBe('recording')
      record.mockImplementation(() => {})
      controller.retryPendingRecords()
      await flush()
      expect(controller.pendingRecords).toHaveLength(0)
      expect(record.mock.calls[1][0]).toBe(attempt)
    },
  )
  it('does not await a slow sink or send concurrent duplicate handoffs', async () => {
    const gate = deferred<void>()
    const record = vi.fn(() => gate.promise)
    const { controller, complete } = setup({ sink: { record } })
    const result = controller.start()
    await flush()
    complete()
    await result
    await flush()
    controller.retryPendingRecords()
    controller.retryPendingRecords()
    await flush()
    expect(record).toHaveBeenCalledOnce()
    expect(controller.pendingRecords).toHaveLength(1)
    gate.resolve()
    await flush()
    expect(controller.pendingRecords).toHaveLength(0)
  })
  it('survives throwing and rejecting UI handlers without losing the attempt', async () => {
    const { controller, complete } = setup({
      onIssue: () => {
        throw new Error('diagnostic failed')
      },
    })
    const result = controller.start({
      onTargetChange: () => {
        throw new Error('render failed')
      },
      onProgress: () => Promise.reject(new Error('progress failed')),
    })
    await flush()
    complete()
    await result
    await flush()
    expect(controller.currentAttempt?.outcome).toBe('COMPLETED')
    expect(controller.issues.length).toBeLessThanOrEqual(50)
  })
  it('protects session boundaries in the development sink', async () => {
    const sink = collectCalibrationAttempts('B')
    const { controller, complete } = setup({ sink })
    const result = controller.start()
    await flush()
    complete()
    await result
    await flush()
    expect(sink.attempts).toHaveLength(0)
    expect(controller.pendingRecords).toHaveLength(1)
  })
  it('rejects duplicate generated IDs rather than overwriting an earlier attempt', async () => {
    const { controller, complete } = setup({ createAttemptId: () => 'same' })
    const result = controller.start()
    await flush()
    complete()
    await result
    await expect(controller.retry()).rejects.toThrow('unique')
    expect(controller.attempts).toHaveLength(1)
  })
  it('does not retain external mutations to session context', async () => {
    const session = { sessionId: 'original', consentGiven: true, eyeTrackingEnabled: true }
    const engine = new FakeEngine()
    const controller = new CalibrationController({
      session,
      engine,
      readViewport: () => ({ width: 800, height: 600, devicePixelRatio: 1 }),
    })
    session.sessionId = 'other'
    const result = controller.start()
    await flush()
    controller.skip()
    expect((await result).sessionId).toBe('original')
  })
})

describe('Preflight records without camera startup', () => {
  it.each(['NO_CAMERA_API', 'NO_CAMERA_DEVICE', 'PERMISSION_DENIED'] as const)(
    'records %s without initializing an unavailable camera',
    async (reason) => {
      const { controller, engine } = setup()
      engine.probe.mockResolvedValue(reason)
      expect(await controller.start()).toMatchObject({
        outcome: 'UNAVAILABLE',
        unavailableReason: reason,
        sessionId: 'session-A',
      })
      expect(engine.start).not.toHaveBeenCalled()
      expect(engine.clearTrainingData).not.toHaveBeenCalled()
      expect(controller.pendingRecords).toHaveLength(1)
    },
  )
  it('records a failed availability probe without blocking the participant', async () => {
    const { controller, engine } = setup()
    engine.probe.mockRejectedValue(new Error('device query failed'))
    expect(await controller.start()).toMatchObject({
      outcome: 'UNAVAILABLE',
      unavailableReason: 'INIT_FAILED',
    })
  })
})
