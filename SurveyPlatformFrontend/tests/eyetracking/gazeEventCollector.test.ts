import { describe, expect, it, vi } from 'vitest'
import { GazeEventCollector, createGazeBufferPort } from '../../src/eyetracking'
import type { GazeEventBatch, GazeEventSink } from '../../src/eyetracking'
import { deferred } from './helpers'
import {
  WALL_ORIGIN,
  attemptFixture,
  fakeElement,
  fakeEnvironment,
  setupCollector,
  settle,
} from './gazeHelpers'

describe('Gaze event collection', () => {
  it('records one complete event per valid prediction', async () => {
    const { collector, sink, world, emit } = setupCollector()
    world.setHit(() => fakeElement('question-3'))
    world.setViewport({ scrollX: 40, scrollY: 90 })
    collector.start()
    expect(collector.bufferMode).toBe('MAIN_THREAD')
    emit(120, 240)
    emit(130, 250)
    await settle()
    expect(sink.events).toHaveLength(2)
    expect(sink.events[0]).toEqual({
      schemaVersion: 1,
      sessionId: 'session-A',
      pageId: 'page-1',
      x: 120,
      y: 240,
      scrollX: 40,
      scrollY: 90,
      pageX: 160,
      pageY: 330,
      viewportWidth: 1000,
      viewportHeight: 800,
      devicePixelRatio: 2,
      widgetId: 'question-3',
      attribution: 'WIDGET',
      timestamp: new Date(WALL_ORIGIN + 20).toISOString(),
      sessionElapsedMs: 20,
      calibrationAttemptId: 'attempt-1',
      calibrationQuality: 'GOOD',
      calibrationLowQualityReasons: [],
    })
    expect(collector.stats).toMatchObject({
      samplesReceived: 2,
      samplesRejected: 0,
      eventsProduced: 2,
      eventsDiscarded: 0,
      batchesDelivered: 1,
      attribution: { WIDGET: 2, NO_CONTENT: 0, UNRESOLVED: 0 },
    })
  })

  it('refuses a prediction or a viewport reading that cannot be serialized', async () => {
    const { collector, engine, sink, world } = setupCollector()
    collector.start()
    for (const [x, y, time] of [
      [Number.NaN, 5, 10],
      [5, Number.POSITIVE_INFINITY, 10],
      [5, 5, Number.NaN],
    ])
      engine.emit(x, y, time)
    world.setViewport({ scrollY: Number.NaN })
    engine.emit(5, 5, 11)
    await settle()
    expect(sink.events).toHaveLength(0)
    expect(collector.stats).toMatchObject({
      samplesReceived: 4,
      samplesRejected: 4,
      eventsProduced: 0,
    })
    expect(collector.issues.map((issue) => issue.stage)).toEqual(['viewport'])
  })

  it('keeps a point that cannot be attributed, with its coordinates intact', async () => {
    const { collector, sink, world, emit } = setupCollector()
    world.setHit(() => {
      throw new Error('layout unavailable')
    })
    collector.start()
    emit(11, 22)
    collector.flushNow()
    await settle()
    expect(sink.events).toHaveLength(1)
    expect(sink.events[0]).toMatchObject({
      x: 11,
      y: 22,
      widgetId: null,
      attribution: 'UNRESOLVED',
    })
    expect(collector.issues[0]?.stage).toBe('attribution')
  })

  it('separates a point on no element from a point outside any widget', async () => {
    const { collector, sink, world, emit } = setupCollector({ batchSize: 3 })
    const chrome = fakeElement(null, fakeElement(null))
    world.setHit((x) => (x < 0 ? null : x < 100 ? chrome : fakeElement('question-3')))
    collector.start()
    emit(-5, 10)
    emit(50, 10)
    emit(500, 10)
    await settle()
    expect(sink.events.map((event) => event.attribution)).toEqual([
      'NO_CONTENT',
      'UNRESOLVED',
      'WIDGET',
    ])
    expect(sink.events.map((event) => event.widgetId)).toEqual([null, null, 'question-3'])
    expect(collector.stats.attribution).toEqual({ WIDGET: 1, NO_CONTENT: 1, UNRESOLVED: 1 })
  })

  it('stamps the page in force at the time of the prediction', async () => {
    const { collector, sink, emit } = setupCollector()
    collector.start()
    emit(1, 1)
    collector.setPage('page-2')
    emit(2, 2)
    await settle()
    expect(sink.events.map((event) => event.pageId)).toEqual(['page-1', 'page-2'])
    expect(collector.pageId).toBe('page-2')
    expect(() => collector.setPage('  ')).toThrow('page ID is required')
  })

  it('puts gaze on the same axis as an event stamped from the shared clock', async () => {
    const { collector, sink, clock, sessionClock, emit } = setupCollector()
    collector.start()
    emit(1, 1)
    clock.time += 5
    const behavioural = sessionClock.now()
    emit(2, 2)
    await settle()
    const [first, second] = sink.events
    expect(first.sessionElapsedMs).toBeLessThan(behavioural.sessionElapsedMs)
    expect(behavioural.sessionElapsedMs).toBeLessThan(second.sessionElapsedMs)
    expect(new Date(first.timestamp).getTime()).toBeLessThan(behavioural.wallMs)
    expect(behavioural.wallMs).toBeLessThan(new Date(second.timestamp).getTime())
    expect(new Set(sink.events.map((event) => event.sessionId))).toEqual(new Set(['session-A']))
  })

  it('offers batches one at a time and numbers them without gaps', async () => {
    let concurrent = 0
    let peak = 0
    const seen: GazeEventBatch[] = []
    const sink: GazeEventSink = {
      deliver: async (batch) => {
        concurrent += 1
        peak = Math.max(peak, concurrent)
        seen.push(batch)
        await Promise.resolve()
        concurrent -= 1
      },
    }
    const { collector, emit } = setupCollector({ sink })
    collector.start()
    for (let index = 0; index < 6; index += 1) emit(index, index)
    await settle()
    expect(peak).toBe(1)
    expect(seen.map((batch) => batch.sequence)).toEqual([1, 2, 3])
    expect(seen.map((batch) => batch.eventCount)).toEqual([2, 2, 2])
  })

  it('counts a rejected batch instead of stalling the rest of the session', async () => {
    const deliver = vi
      .fn<GazeEventSink['deliver']>()
      .mockRejectedValueOnce(new Error('transport offline'))
      .mockResolvedValue(undefined)
    const { collector, emit } = setupCollector({ sink: { deliver } })
    collector.start()
    for (let index = 0; index < 4; index += 1) emit(index, index)
    await settle()
    expect(deliver).toHaveBeenCalledTimes(2)
    expect(collector.stats).toMatchObject({
      eventsProduced: 4,
      batchesDelivered: 1,
      batchesFailed: 1,
      eventsDiscarded: 2,
    })
    expect(collector.issues.at(-1)?.stage).toBe('delivery')
  })

  it('discards the oldest events once the buffer is full, and counts them', async () => {
    const gate = deferred<void>()
    const { collector, emit } = setupCollector({
      sink: { deliver: () => gate.promise },
      batchSize: 2,
      maxEvents: 2,
    })
    collector.start()
    for (let index = 0; index < 5; index += 1) emit(index, index)
    await settle()
    // Nothing has been cut since the first batch, so no batch can carry the news yet.
    expect(collector.stats.eventsDiscarded).toBe(0)
    collector.flushNow()
    await settle()
    // The flush heartbeat reports the loss while the transport is still stuck.
    expect(collector.stats).toMatchObject({ eventsDiscarded: 1, eventsBuffered: 2 })
    gate.resolve()
    await settle()
    expect(collector.stats).toMatchObject({
      eventsProduced: 5,
      eventsDiscarded: 1,
      eventsBuffered: 0,
      batchesDelivered: 2,
    })
  })

  it('stops the engine, empties the buffer and then ignores late predictions', async () => {
    const { collector, engine, sink, emit } = setupCollector()
    collector.start()
    for (let index = 0; index < 3; index += 1) emit(index, index)
    await collector.stop()
    expect(sink.events).toHaveLength(3)
    expect(engine.stop).toHaveBeenCalledOnce()
    expect(engine.setSampleListener).toHaveBeenLastCalledWith(null)
    expect(collector.phase).toBe('STOPPED')
    // The path stays reportable after the buffer is released, for diagnostics.
    expect(collector.bufferMode).toBe('MAIN_THREAD')
    emit(99, 99)
    await settle()
    expect(sink.events).toHaveLength(3)
    expect(collector.stats.samplesReceived).toBe(3)
  })

  it('is safe to stop twice and refuses to start again', async () => {
    const { collector, engine } = setupCollector()
    collector.start()
    const first = collector.stop()
    expect(collector.stop()).toBe(first)
    await first
    await collector.stop()
    expect(engine.stop).toHaveBeenCalledOnce()
    expect(() => collector.start()).toThrow('already been started')
  })

  it('stops an engine it never collected from, exactly once however often it is asked', async () => {
    const { collector, engine } = setupCollector()
    await collector.stop()
    await collector.stop()
    await collector.stop()
    expect(engine.stop).toHaveBeenCalledOnce()
    expect(collector.phase).toBe('STOPPED')
    expect(collector.bufferMode).toBeNull()
  })

  it('gives up draining rather than hanging the participant flow', async () => {
    const { collector, emit } = setupCollector({
      sink: { deliver: () => deferred<void>().promise },
      drainTimeoutMs: 1,
    })
    collector.start()
    emit(1, 1)
    emit(2, 2)
    await collector.stop()
    expect(collector.issues.at(-1)?.stage).toBe('drain')
    expect(collector.phase).toBe('STOPPED')
  })

  it('refuses to start without the session, consent and calibration it needs', () => {
    const world = fakeEnvironment()
    const base = {
      engine: setupCollector().engine,
      sink: { deliver: () => {} },
      clock: setupCollector().sessionClock,
      pageId: 'page-1',
      environment: world.environment,
    }
    const session = { sessionId: 'session-A', consentGiven: true, eyeTrackingEnabled: true }
    expect(
      () =>
        new GazeEventCollector({ ...base, session: { ...session, sessionId: ' ' }, calibration: attemptFixture() }),
    ).toThrow('participant session ID')
    expect(
      () =>
        new GazeEventCollector({
          ...base,
          session: { ...session, consentGiven: false },
          calibration: attemptFixture(),
        }),
    ).toThrow('consent')
    expect(
      () =>
        new GazeEventCollector({
          ...base,
          session: { ...session, eyeTrackingEnabled: false },
          calibration: attemptFixture(),
        }),
    ).toThrow('consent')
    expect(
      () =>
        new GazeEventCollector({
          ...base,
          session,
          calibration: attemptFixture({ sessionId: 'session-B' }),
        }),
    ).toThrow('different participant session')
    expect(
      () =>
        new GazeEventCollector({
          ...base,
          session,
          calibration: attemptFixture({ trackingAvailable: false }),
        }),
    ).toThrow('available engine')
    expect(
      () => new GazeEventCollector({ ...base, session, calibration: attemptFixture(), pageId: '' }),
    ).toThrow('page ID is required')
    expect(
      () =>
        new GazeEventCollector({
          ...base,
          session,
          calibration: attemptFixture(),
          flushIntervalMs: -1,
        }),
    ).toThrow('flushIntervalMs')
    expect(
      () =>
        new GazeEventCollector({
          ...base,
          session,
          calibration: attemptFixture(),
          drainTimeoutMs: 0,
        }),
    ).toThrow('drainTimeoutMs')
  })

  it('cuts a partial batch on the flush cadence', async () => {
    const { collector, sink, emit } = setupCollector({ batchSize: 50 })
    collector.start()
    emit(1, 1)
    await settle()
    expect(sink.batches).toHaveLength(0)
    collector.flushNow()
    await settle()
    expect(sink.batches).toHaveLength(1)
    expect(sink.batches[0].eventCount).toBe(1)
  })
})

describe('Buffer placement', () => {
  it('degrades to the main thread where a module worker cannot be created', () => {
    const issues: unknown[] = []
    const port = createGazeBufferPort(
      { sessionId: 'session-A', batchSize: 1 },
      {
        onBatch: () => {},
        onDrained: () => {},
        onStats: () => {},
        onIssue: (error) => issues.push(error),
        onDiscard: () => {},
      },
    )
    expect(port.mode).toBe('MAIN_THREAD')
    expect(issues).toHaveLength(1)
    port.close()
  })
})
