// @vitest-environment happy-dom

import { AxiosError } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { BehaviorUploadSession } from '../../src/interaction/behaviorUploader'
import type { BehaviorBatch } from '../../src/interaction/behaviorUploader'
import type { BehaviorEvent } from '../../src/interaction/behaviorCollector'

const mocks = vi.hoisted(() => ({
  post: vi.fn(), save: vi.fn(), load: vi.fn(), remove: vi.fn(),
  events: [] as BehaviorEvent[],
  stored: new Map<string, { sessionId: string; batch: BehaviorBatch }>(),
}))
vi.mock('../../src/participant/api/participantHttpClient', () => ({ default: { post: mocks.post } }))
vi.mock('../../src/interaction/behaviorCollector', () => ({
  getBehaviorEvents: () => mocks.events.map(event => ({ ...event })),
  takeBehaviorEvents: (limit: number) => mocks.events.splice(0, limit),
}))
vi.mock('../../src/interaction/behaviorQueue', () => ({
  savePendingBehaviorBatch: mocks.save,
  loadPendingBehaviorBatches: mocks.load,
  deletePendingBehaviorBatch: mocks.remove,
}))

const owners: BehaviorUploadSession[] = []
function owner() {
  const result = new BehaviorUploadSession({ sessionId: 'session-A', sessionToken: 'token-A', batchSize: 2, flushIntervalMs: 1000 })
  owners.push(result)
  return result
}
function enqueue(count = 1) {
  for (let index = 0; index < count; index += 1) mocks.events.push({
    type: 'click', timestamp: 1_700_000_000_000 + index, sessionElapsedMs: index,
    pageId: 'feed', x: 120, y: 240, componentId: 'post:like', componentType: 'PostLike',
  })
}
function acknowledgement(batch: BehaviorBatch) {
  return { data: { sessionId: 'session-A', batchId: batch.batchId, streamId: batch.streamId,
    sequence: batch.sequence, eventCount: batch.events.length, receivedAt: '2026-11-01T00:00:00Z' }, status: 201 }
}
async function settle() { for (let index = 0; index < 30; index += 1) await Promise.resolve() }

beforeEach(() => {
  vi.useFakeTimers()
  vi.clearAllMocks()
  mocks.events.length = 0
  mocks.stored.clear()
  mocks.save.mockImplementation(async (sessionId: string, batch: BehaviorBatch) => {
    mocks.stored.set(batch.batchId, { sessionId, batch: structuredClone(batch) })
  })
  mocks.load.mockImplementation(async (sessionId: string) => [...mocks.stored.values()].filter(record => record.sessionId === sessionId))
  mocks.remove.mockImplementation(async (id: string) => { mocks.stored.delete(id) })
  mocks.post.mockImplementation(async (_url: string, batch: BehaviorBatch) => acknowledgement(batch))
  vi.spyOn(console, 'log').mockImplementation(() => {})
  vi.spyOn(console, 'warn').mockImplementation(() => {})
})
afterEach(() => {
  for (const session of owners.splice(0)) session.stopAutoFlush()
  vi.useRealTimers()
  vi.restoreAllMocks()
})

describe('Behavior delivery regression', () => {
  it('persists batches before sending and preserves event order across batch boundaries', async () => {
    enqueue(5)
    const session = owner()
    mocks.post.mockImplementation(async (_url: string, batch: BehaviorBatch) => {
      expect(mocks.stored.get(batch.batchId)?.batch).toEqual(batch)
      return acknowledgement(batch)
    })
    await session.flush()
    const batches = mocks.post.mock.calls.map(call => call[1] as BehaviorBatch)
    expect(batches.map(batch => batch.events.length)).toEqual([2, 2, 1])
    expect(batches.map(batch => batch.sequence)).toEqual([1, 2, 3])
    expect(new Set(batches.map(batch => batch.streamId)).size).toBe(1)
    expect(batches.flatMap(batch => batch.events.map(event => event.sessionElapsedMs))).toEqual([0, 1, 2, 3, 4])
    expect(mocks.post.mock.calls[0][0]).toBe('/api/participant-session/behavior-batches')
    expect(mocks.post.mock.calls[0][2].headers['X-Participant-Session-Token']).toBe('token-A')
    expect(mocks.stored.size).toBe(0)
    expect(session.getPendingBatchCount()).toBe(0)
  })

  it('retries the same batch after a network failure and removes it once acknowledged', async () => {
    enqueue()
    mocks.post.mockRejectedValueOnce(new AxiosError('offline'))
    const pending = owner().flush()
    await settle()
    expect(mocks.remove).not.toHaveBeenCalled()
    await vi.advanceTimersByTimeAsync(250)
    await pending
    expect(mocks.post).toHaveBeenCalledTimes(2)
    expect(mocks.post.mock.calls[1][1]).toEqual(mocks.post.mock.calls[0][1])
    expect(mocks.remove).toHaveBeenCalledTimes(1)
  })

  it('retains a batch after retries are exhausted and sends it unchanged on a later flush', async () => {
    enqueue(2)
    const session = owner()
    mocks.post.mockRejectedValue(new AxiosError('offline'))
    const failed = expect(session.flush()).rejects.toThrow('offline')
    await vi.advanceTimersByTimeAsync(750)
    await failed
    expect(mocks.post).toHaveBeenCalledTimes(3)
    expect(session.getPendingBatchCount()).toBe(1)
    expect(mocks.stored.size).toBe(1)
    const original = structuredClone(mocks.post.mock.calls[0][1])
    mocks.post.mockImplementation(async (_url: string, batch: BehaviorBatch) => acknowledgement(batch))
    await session.flush()
    expect(mocks.post.mock.calls[3][1]).toEqual(original)
    expect(mocks.stored.size).toBe(0)
    expect(session.getPendingBatchCount()).toBe(0)
  })

  it('restores an old stream before sending events from a new upload session', async () => {
    const old: BehaviorBatch = { schemaVersion: 1, batchId: 'old-batch', streamId: 'old-stream', sequence: 17, events: [] }
    enqueue()
    old.events = mocks.events.splice(0)
    mocks.stored.set(old.batchId, { sessionId: 'session-A', batch: old })
    enqueue()
    await owner().flush()
    expect(mocks.post.mock.calls[0][1]).toEqual(old)
    const fresh = mocks.post.mock.calls[1][1] as BehaviorBatch
    expect(fresh.streamId).not.toBe(old.streamId)
    expect(fresh.sequence).toBe(1)
    expect(mocks.stored.size).toBe(0)
  })

  it.each(['batchId', 'streamId', 'sequence', 'eventCount'] as const)(
    'retains a batch when acknowledgement %s does not match', async field => {
      enqueue()
      mocks.post.mockImplementation(async (_url: string, batch: BehaviorBatch) => {
        const response = acknowledgement(batch)
        Object.assign(response.data, { [field]: 'wrong' })
        return response
      })
      const session = owner()
      await expect(session.flush()).rejects.toThrow('acknowledgement did not match')
      expect(session.getPendingBatchCount()).toBe(1)
      expect(mocks.remove).not.toHaveBeenCalled()
    },
  )

  it('shares an in-flight flush so concurrent callers do not send a batch twice', async () => {
    enqueue()
    let resolve!: (value: ReturnType<typeof acknowledgement>) => void
    mocks.post.mockImplementation(() => new Promise(done => { resolve = done }))
    const session = owner()
    const first = session.flush()
    const second = session.flush()
    await settle()
    expect(mocks.post).toHaveBeenCalledTimes(1)
    resolve(acknowledgement(mocks.post.mock.calls[0][1]))
    await Promise.all([first, second])
    expect(mocks.remove).toHaveBeenCalledTimes(1)
  })

  it('flushes on reconnect and removes its timer and online listener when stopped', async () => {
    const session = owner()
    session.startAutoFlush()
    session.startAutoFlush()
    await settle()
    enqueue()
    window.dispatchEvent(new Event('online'))
    await settle()
    expect(mocks.post).toHaveBeenCalledTimes(1)
    session.stopAutoFlush()
    enqueue()
    window.dispatchEvent(new Event('online'))
    await vi.advanceTimersByTimeAsync(2000)
    expect(mocks.post).toHaveBeenCalledTimes(1)
  })
})
