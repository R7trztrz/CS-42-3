import { describe, expect, it, vi } from 'vitest'
import { WebGazerGazeEngine } from '../../src/eyetracking'
import type { WebGazerLike, WebGazerPrediction } from '../../src/eyetracking'
import { deferred, flush } from './helpers'

class FakeWebGazer implements WebGazerLike {
  callback: ((sample: WebGazerPrediction | null, time: number) => void) | null = null
  saveDataAcrossSessions = vi.fn()
  setRegression = vi.fn()
  init = vi.fn()
  getRegression = vi.fn(() => [{ init: this.init }])
  clearData = vi.fn()
  setGazeListener = vi.fn((callback: (sample: WebGazerPrediction | null, time: number) => void) => {
    this.callback = callback
  })
  clearGazeListener = vi.fn(() => {
    this.callback = null
  })
  begin = vi.fn(async () => {})
  end = vi.fn()
  stopVideo = vi.fn()
  recordScreenPosition = vi.fn()
  removeMouseEventListeners = vi.fn()
}

function fixture() {
  const webgazer = new FakeWebGazer()
  const engine = new WebGazerGazeEngine({ webgazer, now: () => 10 })
  const abort = new AbortController()
  return { webgazer, engine, abort }
}

describe('WebGazer 3.5.3 adapter contract', () => {
  it('makes implicit mouse training and camera cleanup APIs mandatory', () => {
    const invalid = new FakeWebGazer()
    Object.assign(invalid, { removeMouseEventListeners: undefined })
    expect(() => new WebGazerGazeEngine({ webgazer: invalid })).toThrow('removeMouseEventListeners')
  })
  it('resets regression without clearing the origin-wide localforage store', async () => {
    const { engine, webgazer } = fixture()
    await engine.clearTrainingData()
    expect(webgazer.saveDataAcrossSessions).toHaveBeenCalledWith(false)
    expect(webgazer.init).toHaveBeenCalledOnce()
    expect(webgazer.clearData).not.toHaveBeenCalled()
    engine.stop()
  })
  it('awaits a regression reset and propagates its failure', async () => {
    const { engine, webgazer } = fixture()
    const gate = deferred<void>()
    webgazer.init.mockReturnValue(gate.promise)
    let finished = false
    const result = engine.clearTrainingData().then(() => {
      finished = true
    })
    await flush()
    expect(finished).toBe(false)
    gate.resolve()
    await result
    expect(finished).toBe(true)
    webgazer.init.mockImplementation(() => {
      throw new Error('bad regression')
    })
    await expect(engine.clearTrainingData()).rejects.toThrow('bad regression')
    engine.stop()
  })
  it('filters invalid predictions and replaces the listener without restarting the model', async () => {
    const { engine, webgazer, abort } = fixture()
    const first = vi.fn()
    const second = vi.fn()
    await engine.clearTrainingData()
    await engine.start(first, abort.signal)
    for (const data of [null, { x: NaN, y: 3 }, { x: 1, y: Infinity }])
      webgazer.callback?.(data, 900)
    expect(first).not.toHaveBeenCalled()
    engine.setSampleListener(second)
    webgazer.callback?.({ x: 1, y: 2 }, 900)
    expect(second).toHaveBeenCalledWith({ x: 1, y: 2, timestamp: 10 })
    expect(webgazer.begin).toHaveBeenCalledOnce()
    expect(webgazer.init).toHaveBeenCalledOnce()
    engine.stop()
    expect(webgazer.stopVideo).toHaveBeenCalledOnce()
    expect(webgazer.end).toHaveBeenCalledOnce()
    engine.stop()
    expect(webgazer.end).toHaveBeenCalledOnce()
  })
  it('deduplicates overlapping adapter starts', async () => {
    const { engine, webgazer, abort } = fixture()
    const gate = deferred<void>()
    webgazer.begin.mockReturnValue(gate.promise)
    const one = engine.start(vi.fn(), abort.signal)
    const two = engine.start(vi.fn(), abort.signal)
    gate.resolve()
    await Promise.all([one, two])
    expect(webgazer.begin).toHaveBeenCalledOnce()
    engine.stop()
  })
  it.each(['sync', 'async'])('isolates a %s failing prediction consumer', async (kind) => {
    const webgazer = new FakeWebGazer()
    const onIssue = vi.fn()
    const engine = new WebGazerGazeEngine({ webgazer, onIssue })
    const failure = new Error('collector unavailable')
    const consumer = () => {
      if (kind === 'sync') throw failure
      return Promise.reject(failure)
    }
    await engine.start(consumer, new AbortController().signal)
    expect(() => webgazer.callback?.({ x: 1, y: 2 }, 0)).not.toThrow()
    await flush()
    expect(onIssue).toHaveBeenCalledWith(failure)
    const next = vi.fn()
    engine.setSampleListener(next)
    webgazer.callback?.({ x: 3, y: 4 }, 0)
    expect(next).toHaveBeenCalledOnce()
    engine.stop()
    expect(webgazer.stopVideo).toHaveBeenCalled()
  })
  it('cleans partial initialization failures and classifies denied permission', async () => {
    const { engine, webgazer, abort } = fixture()
    webgazer.begin.mockRejectedValue(new DOMException('denied', 'NotAllowedError'))
    await expect(engine.start(vi.fn(), abort.signal)).rejects.toMatchObject({
      reason: 'PERMISSION_DENIED',
    })
    expect(webgazer.stopVideo).toHaveBeenCalled()
    expect(webgazer.clearGazeListener).toHaveBeenCalled()
    expect(webgazer.end).toHaveBeenCalled()
    await expect(engine.start(vi.fn(), abort.signal)).rejects.toThrow('disposed')
  })
  it('cleans up again when a cancelled begin finishes late', async () => {
    const { engine, webgazer, abort } = fixture()
    const gate = deferred<void>()
    webgazer.begin.mockReturnValue(gate.promise)
    const result = engine.start(vi.fn(), abort.signal)
    const rejection = expect(result).rejects.toMatchObject({ reason: 'INIT_CANCELLED' })
    abort.abort()
    gate.resolve()
    await rejection
    expect(webgazer.stopVideo).toHaveBeenCalled()
    expect(webgazer.end).toHaveBeenCalled()
  })
  it('prevents two session engines from manipulating the same singleton', async () => {
    const { engine, webgazer } = fixture()
    const second = new WebGazerGazeEngine({ webgazer })
    await engine.clearTrainingData()
    await expect(second.clearTrainingData()).rejects.toThrow('another session')
    second.stop()
    expect(webgazer.clearGazeListener).not.toHaveBeenCalled()
    engine.stop()
    const third = new WebGazerGazeEngine({ webgazer })
    await third.clearTrainingData()
    third.stop()
  })
  it.each(['NotFoundError', 'OverconstrainedError', 'SecurityError', 'NotReadableError'])(
    'classifies %s without throwing it through the controller contract',
    async (name) => {
      const { engine, webgazer, abort } = fixture()
      webgazer.begin.mockRejectedValue(new DOMException('camera unavailable', name))
      const reason =
        name === 'SecurityError'
          ? 'PERMISSION_DENIED'
          : name === 'NotReadableError'
            ? 'INIT_FAILED'
            : 'NO_CAMERA_DEVICE'
      await expect(engine.start(vi.fn(), abort.signal)).rejects.toMatchObject({ reason })
    },
  )
  it('does not start on an already aborted signal', async () => {
    const { engine, webgazer, abort } = fixture()
    abort.abort()
    await expect(engine.start(vi.fn(), abort.signal)).rejects.toMatchObject({
      reason: 'INIT_CANCELLED',
    })
    expect(webgazer.begin).not.toHaveBeenCalled()
  })
})

describe('Non-prompting camera probe', () => {
  function navigatorFixture(
    state: PermissionState = 'prompt',
    devices: Partial<MediaDeviceInfo>[] = [],
  ) {
    return {
      permissions: { query: vi.fn(async () => ({ state })) },
      mediaDevices: { getUserMedia: vi.fn(), enumerateDevices: vi.fn(async () => devices) },
    }
  }
  it('does not request permission during a probe', async () => {
    const navigator = navigatorFixture()
    const engine = new WebGazerGazeEngine({
      webgazer: new FakeWebGazer(),
      navigator: navigator as unknown as Navigator,
    })
    expect(await engine.probe()).toBeNull()
    expect(navigator.mediaDevices.getUserMedia).not.toHaveBeenCalled()
  })
  it('reports a missing API, denied permission and a known absent camera', async () => {
    const missing = new WebGazerGazeEngine({
      webgazer: new FakeWebGazer(),
      navigator: {} as Navigator,
    })
    expect(await missing.probe()).toBe('NO_CAMERA_API')
    const denied = new WebGazerGazeEngine({
      webgazer: new FakeWebGazer(),
      navigator: navigatorFixture('denied') as unknown as Navigator,
    })
    expect(await denied.probe()).toBe('PERMISSION_DENIED')
    const absent = new WebGazerGazeEngine({
      webgazer: new FakeWebGazer(),
      navigator: navigatorFixture('granted', [{ kind: 'audioinput' }]) as unknown as Navigator,
    })
    expect(await absent.probe()).toBe('NO_CAMERA_DEVICE')
  })
  it('does not turn unsupported permission queries or enumeration into a false unavailable result', async () => {
    const navigator = navigatorFixture()
    navigator.permissions.query.mockRejectedValue(new Error('unsupported'))
    navigator.mediaDevices.enumerateDevices.mockRejectedValue(new Error('hidden before permission'))
    const engine = new WebGazerGazeEngine({
      webgazer: new FakeWebGazer(),
      navigator: navigator as unknown as Navigator,
    })
    expect(await engine.probe()).toBeNull()
  })
})
