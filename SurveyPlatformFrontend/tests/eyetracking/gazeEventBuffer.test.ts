import { describe, expect, it, vi } from 'vitest'
import {
  DEFAULT_BATCH_SIZE,
  DEFAULT_MAX_BUFFERED_EVENTS,
  GazeEventBuffer,
  createGazeBufferHost,
} from '../../src/eyetracking'
import type {
  GazeBufferStats,
  GazeEvent,
  GazeEventBatch,
  GazeEventBufferOptions,
} from '../../src/eyetracking'

const INVALID_OPTIONS: [GazeEventBufferOptions, string][] = [
  [{ sessionId: '  ' }, 'participant session ID'],
  [{ sessionId: 'session-A', maxEvents: 0 }, 'maxEvents'],
  [{ sessionId: 'session-A', maxEvents: 1.5 }, 'maxEvents'],
  [{ sessionId: 'session-A', maxEvents: 2, batchSize: 3 }, 'batchSize'],
  [{ sessionId: 'session-A', batchSize: 0 }, 'batchSize'],
  [{ sessionId: 'session-A', initialSequence: -1 }, 'initialSequence'],
  [{ sessionId: 'session-A', initialDroppedTotal: Number.NaN }, 'initialDroppedTotal'],
]

function event(index: number): GazeEvent {
  return Object.freeze({
    schemaVersion: 1,
    sessionId: 'session-A',
    pageId: 'page-1',
    x: index,
    y: index * 2,
    scrollX: 0,
    scrollY: 0,
    pageX: index,
    pageY: index * 2,
    viewportWidth: 1000,
    viewportHeight: 800,
    devicePixelRatio: 2,
    widgetId: null,
    attribution: 'UNRESOLVED',
    timestamp: '2026-09-21T00:00:00.000Z',
    sessionElapsedMs: index,
    calibrationAttemptId: 'attempt-1',
    calibrationQuality: 'GOOD',
    calibrationLowQualityReasons: Object.freeze([]),
  })
}

function parse(batch: GazeEventBatch): GazeEvent[] {
  return JSON.parse(batch.payload) as GazeEvent[]
}

function buffer(overrides: { maxEvents?: number; batchSize?: number } = {}) {
  let id = 0
  return new GazeEventBuffer({
    sessionId: 'session-A',
    batchSize: 3,
    maxEvents: 5,
    createBatchId: () => `batch-${++id}`,
    ...overrides,
  })
}

describe('Gaze event buffer', () => {
  it('publishes conservative defaults that the host can override', () => {
    expect(DEFAULT_MAX_BUFFERED_EVENTS).toBe(5000)
    expect(DEFAULT_BATCH_SIZE).toBe(200)
    const defaults = new GazeEventBuffer({ sessionId: 'session-A' })
    expect(defaults.capacity).toBe(5000)
    expect(defaults.batchSize).toBe(200)
  })

  it('cuts at the batch size and serializes exactly the events it removed', () => {
    const queue = buffer()
    expect(queue.add(event(1))).toBeNull()
    expect(queue.add(event(2))).toBeNull()
    const batch = queue.add(event(3))
    expect(batch).toMatchObject({
      schemaVersion: 1,
      sessionId: 'session-A',
      batchId: 'batch-1',
      sequence: 1,
      eventCount: 3,
      droppedEvents: 0,
      droppedEventsTotal: 0,
    })
    expect(parse(batch!).map((item) => item.x)).toEqual([1, 2, 3])
    expect(queue.pendingCount).toBe(0)
    expect(Object.isFrozen(batch)).toBe(true)
  })

  it('keeps only one batch outstanding until the transport owner settles it', () => {
    const queue = buffer()
    queue.add(event(1))
    queue.add(event(2))
    expect(queue.add(event(3))).not.toBeNull()
    expect(queue.hasOutstandingBatch).toBe(true)
    expect(queue.add(event(4))).toBeNull()
    expect(queue.add(event(5))).toBeNull()
    expect(queue.flush()).toBeNull()
    const next = queue.release()
    expect(next).toBeNull()
    expect(queue.pendingCount).toBe(2)
    expect(queue.flush()?.sequence).toBe(2)
  })

  it('cuts immediately on release when a full batch is already waiting', () => {
    const queue = buffer({ batchSize: 2, maxEvents: 10 })
    queue.add(event(1))
    expect(queue.add(event(2))?.sequence).toBe(1)
    queue.add(event(3))
    queue.add(event(4))
    const next = queue.release()
    expect(next?.sequence).toBe(2)
    expect(parse(next!).map((item) => item.x)).toEqual([3, 4])
  })

  it('numbers batches gap-free and never repeats an event', () => {
    const queue = buffer({ batchSize: 1, maxEvents: 10 })
    const seen: number[] = []
    for (let index = 1; index <= 4; index += 1) {
      const batch = queue.add(event(index))
      if (batch) {
        seen.push(batch.sequence)
        expect(parse(batch)).toHaveLength(1)
      }
      queue.release()
    }
    expect(seen).toEqual([1, 2, 3, 4])
    expect(queue.lastSequence).toBe(4)
  })

  it('drops the oldest event on overflow and reports the loss per batch and cumulatively', () => {
    const queue = buffer({ batchSize: 2, maxEvents: 2 })
    expect(queue.add(event(1))).toBeNull()
    // The queue only grows while a batch is outstanding, which is where the limit has to bite.
    expect(queue.add(event(2))?.sequence).toBe(1)
    queue.add(event(3))
    queue.add(event(4))
    queue.add(event(5))
    expect(queue.pendingCount).toBe(2)
    expect(queue.droppedEventsTotal).toBe(1)
    const second = queue.release()
    expect(second).toMatchObject({ droppedEvents: 1, droppedEventsTotal: 1, eventCount: 2 })
    expect(parse(second!).map((item) => item.x)).toEqual([4, 5])
    queue.add(event(6))
    queue.add(event(7))
    queue.add(event(8))
    const third = queue.release()
    expect(third).toMatchObject({ droppedEvents: 1, droppedEventsTotal: 2 })
    expect(parse(third!).map((item) => item.x)).toEqual([7, 8])
  })

  it('continues the sequence and the dropped total after a failover', () => {
    const queue = buffer({ batchSize: 1 })
    const resumed = new GazeEventBuffer({
      sessionId: 'session-A',
      batchSize: 1,
      initialSequence: 7,
      initialDroppedTotal: 4,
    })
    expect(queue.lastSequence).toBe(0)
    expect(resumed.add(event(1))).toMatchObject({ sequence: 8, droppedEventsTotal: 4 })
  })

  it('generates a batch ID when the host supplies none', () => {
    const uuid = vi.spyOn(globalThis.crypto, 'randomUUID')
    const generated = new GazeEventBuffer({ sessionId: 'session-A', batchSize: 1 }).add(event(1))
    expect(uuid).toHaveBeenCalledOnce()
    expect(generated?.batchId).toBe(uuid.mock.results[0]?.value)
  })

  it('derives a batch ID where randomUUID is unavailable', () => {
    vi.stubGlobal('crypto', {})
    try {
      const derived = new GazeEventBuffer({ sessionId: 'session-A', batchSize: 1 }).add(event(1))
      expect(derived?.batchId).toBe('session-A-batch-1')
    } finally {
      vi.unstubAllGlobals()
    }
  })

  it.each(INVALID_OPTIONS)('rejects the invalid option set %j', (options, message) => {
    expect(() => new GazeEventBuffer(options)).toThrow(message)
  })
})

describe('Gaze buffer host', () => {
  function host(options: { batchSize?: number; maxEvents?: number } = {}) {
    const batches: GazeEventBatch[] = []
    const reports: GazeBufferStats[] = []
    const drained = vi.fn()
    const instance = createGazeBufferHost(
      { sessionId: 'session-A', batchSize: 2, maxEvents: 10, ...options },
      { batch: (batch) => batches.push(batch), drained, stats: (stats) => reports.push(stats) },
    )
    return { instance, batches, drained, reports }
  }

  it('emits a batch as soon as one can be cut', () => {
    const { instance, batches } = host()
    instance.add(event(1))
    expect(batches).toHaveLength(0)
    instance.add(event(2))
    expect(batches).toHaveLength(1)
    instance.add(event(3))
    instance.add(event(4))
    expect(batches).toHaveLength(1)
    instance.release()
    expect(batches).toHaveLength(2)
  })

  it('reports drained only once nothing is queued and nothing is outstanding', () => {
    const { instance, batches, drained } = host()
    instance.add(event(1))
    instance.drain()
    expect(batches).toHaveLength(1)
    expect(drained).not.toHaveBeenCalled()
    instance.release()
    expect(drained).toHaveBeenCalledOnce()
  })

  it('keeps cutting partial batches while draining until the queue is empty', () => {
    const { instance, batches, drained } = host({ batchSize: 2 })
    for (let index = 1; index <= 5; index += 1) instance.add(event(index))
    expect(batches).toHaveLength(1)
    instance.drain()
    while (!drained.mock.calls.length) instance.release()
    expect(batches.flatMap((batch) => parse(batch).map((item) => item.x))).toEqual([1, 2, 3, 4, 5])
    expect(batches.map((batch) => batch.sequence)).toEqual([1, 2, 3])
  })

  it('drains immediately when nothing was ever buffered', () => {
    const { instance, batches, drained } = host()
    instance.drain()
    expect(batches).toHaveLength(0)
    expect(drained).toHaveBeenCalledOnce()
  })

  it('does nothing on a flush with an empty queue', () => {
    const { instance, batches, drained } = host()
    instance.flush()
    expect(batches).toHaveLength(0)
    expect(drained).not.toHaveBeenCalled()
  })

  it('reports queue depth and overflow on every flush, cut or not', () => {
    const { instance, batches, reports } = host({ batchSize: 2, maxEvents: 2 })
    instance.flush()
    expect(reports.at(-1)).toEqual({ pendingCount: 0, droppedEventsTotal: 0 })
    instance.add(event(1))
    instance.add(event(2)) // cut; now outstanding, so nothing else can leave
    expect(batches).toHaveLength(1)
    for (let index = 3; index <= 6; index += 1) instance.add(event(index))
    instance.flush()
    // The transport is stuck, so no batch carries the news; the heartbeat still does.
    expect(batches).toHaveLength(1)
    expect(reports.at(-1)).toEqual({ pendingCount: 2, droppedEventsTotal: 2 })
    expect(Object.isFrozen(reports.at(-1))).toBe(true)
  })
})
