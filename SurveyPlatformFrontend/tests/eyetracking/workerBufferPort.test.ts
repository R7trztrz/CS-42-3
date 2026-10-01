import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createWorkerBufferPort } from '../../src/eyetracking'
import type {
  GazeBufferCommand,
  GazeBufferPortHandlers,
  GazeBufferStats,
  GazeEvent,
  GazeEventBatch,
} from '../../src/eyetracking'
import { settle } from './gazeHelpers'

interface FakeWorker {
  posted: GazeBufferCommand[]
  terminated: number
  addEventListener(type: string, listener: (event: never) => void): void
  removeEventListener(): void
  postMessage(command: GazeBufferCommand): void
  terminate(): void
  /** Plays a message or error back to the port, the way a real worker would. */
  dispatch(type: string, event: unknown): void
}

let lastWorker: FakeWorker | null = null

function createFakeWorker(): FakeWorker {
  const listeners: Record<string, ((event: unknown) => void)[]> = {}
  const worker: FakeWorker = {
    posted: [],
    terminated: 0,
    addEventListener(type, listener) {
      ;(listeners[type] ??= []).push(listener as (event: unknown) => void)
    },
    removeEventListener() {},
    postMessage(command) {
      worker.posted.push(command)
    },
    terminate() {
      worker.terminated += 1
    },
    dispatch(type, event) {
      for (const listener of listeners[type] ?? []) listener(event)
    },
  }
  return worker
}

function event(index: number): GazeEvent {
  return Object.freeze({
    schemaVersion: 1,
    sessionId: 'session-A',
    pageId: 'page-1',
    x: index,
    y: index,
    scrollX: 0,
    scrollY: 0,
    pageX: index,
    pageY: index,
    viewportWidth: 1000,
    viewportHeight: 800,
    devicePixelRatio: 1,
    widgetId: null,
    attribution: 'UNRESOLVED',
    timestamp: '2026-09-21T00:00:00.000Z',
    sessionElapsedMs: index,
    calibrationAttemptId: 'attempt-1',
    calibrationQuality: 'GOOD',
    calibrationLowQualityReasons: Object.freeze([]),
  })
}

function workerBatch(sequence: number, eventCount: number, droppedEventsTotal = 0): GazeEventBatch {
  return Object.freeze({
    schemaVersion: 1,
    sessionId: 'session-A',
    batchId: `worker-${sequence}`,
    sequence,
    eventCount,
    droppedEvents: 0,
    droppedEventsTotal,
    payload: '[]',
  })
}

function harness() {
  const batches: GazeEventBatch[] = []
  const issues: unknown[] = []
  const discards: number[] = []
  const onDrained = vi.fn()
  const reports: GazeBufferStats[] = []
  const handlers: GazeBufferPortHandlers = {
    onBatch: (batch) => batches.push(batch),
    onDrained,
    onStats: (stats) => reports.push(stats),
    onIssue: (error) => issues.push(error),
    onDiscard: (count) => discards.push(count),
  }
  const port = createWorkerBufferPort({ sessionId: 'session-A', batchSize: 2 }, handlers)
  return { port, batches, issues, discards, reports, onDrained, worker: lastWorker! }
}

describe('Worker buffer port', () => {
  beforeEach(() => {
    lastWorker = null
    vi.stubGlobal('Worker', function FakeWorkerConstructor() {
      lastWorker = createFakeWorker()
      return lastWorker
    })
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('initializes the worker first and forwards every command in order', () => {
    const { port, worker, batches, onDrained } = harness()
    expect(port.mode).toBe('WORKER')
    expect(worker.posted[0]).toEqual({
      type: 'init',
      options: { sessionId: 'session-A', batchSize: 2, maxEvents: undefined },
    })
    port.add(event(1))
    port.flush()
    port.release()
    port.drain()
    expect(worker.posted.slice(1).map((command) => command.type)).toEqual([
      'add',
      'flush',
      'release',
      'drain',
    ])
    worker.dispatch('message', { data: { type: 'batch', batch: workerBatch(1, 2) } })
    worker.dispatch('message', { data: { type: 'drained' } })
    expect(batches.map((batch) => batch.sequence)).toEqual([1])
    expect(onDrained).toHaveBeenCalledOnce()
  })

  it('fails over to the main thread, counts what the worker still held and keeps numbering', async () => {
    const { port, worker, batches, issues, discards } = harness()
    for (let index = 1; index <= 3; index += 1) port.add(event(index))
    worker.dispatch('message', { data: { type: 'batch', batch: workerBatch(1, 2) } })
    worker.dispatch('error', { message: 'worker died', error: new Error('worker died') })
    expect(port.mode).toBe('MAIN_THREAD')
    expect(worker.terminated).toBe(1)
    expect(issues).toHaveLength(1)
    // Three posted, two came back in a batch, none reported as overflow: one was still inside.
    expect(discards).toEqual([1])
    port.add(event(4))
    port.add(event(5))
    await settle()
    expect(batches.map((batch) => batch.sequence)).toEqual([1, 2])
    expect(worker.posted.filter((command) => command.type === 'add')).toHaveLength(3)
  })

  it('forwards the buffer heartbeat and counts its drops against what the worker still holds', async () => {
    const { port, worker, reports, discards, issues } = harness()
    for (let index = 1; index <= 5; index += 1) port.add(event(index))
    worker.dispatch('message', { data: { type: 'batch', batch: workerBatch(1, 2) } })
    worker.dispatch('message', {
      data: { type: 'stats', stats: { pendingCount: 1, droppedEventsTotal: 2 } },
    })
    expect(reports).toEqual([{ pendingCount: 1, droppedEventsTotal: 2 }])
    worker.dispatch('error', { message: 'worker died' })
    expect(issues).toHaveLength(1)
    // 5 posted − 2 returned − 2 already counted as overflow = 1 still inside the dead worker.
    expect(discards).toEqual([1])
  })

  it('treats a reported worker failure the same way as a crash', async () => {
    const { port, worker, issues, discards, batches } = harness()
    port.add(event(1))
    worker.dispatch('message', { data: { type: 'failed', message: 'buffer rejected an event' } })
    expect(issues).toHaveLength(1)
    expect(String(issues[0])).toContain('buffer rejected an event')
    expect(discards).toEqual([1])
    port.add(event(2))
    port.add(event(3))
    await settle()
    expect(batches.map((batch) => batch.sequence)).toEqual([1])
  })

  it('stops forwarding and terminates the worker once closed', async () => {
    const { port, worker, batches } = harness()
    port.close()
    expect(worker.terminated).toBe(1)
    port.add(event(1))
    port.flush()
    expect(worker.posted).toHaveLength(1)
    worker.dispatch('message', { data: { type: 'batch', batch: workerBatch(1, 1) } })
    await settle()
    expect(batches).toHaveLength(0)
  })

  it('refuses to create a worker port where workers do not exist', () => {
    vi.unstubAllGlobals()
    expect(() =>
      createWorkerBufferPort(
        { sessionId: 'session-A' },
        {
          onBatch: () => {},
          onDrained: () => {},
          onStats: () => {},
          onIssue: () => {},
          onDiscard: () => {},
        },
      ),
    ).toThrow('Web Workers are not available.')
  })
})
