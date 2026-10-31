// @vitest-environment happy-dom

import { IDBFactory, IDBKeyRange } from 'fake-indexeddb'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { deletePendingBehaviorBatch, loadPendingBehaviorBatches, savePendingBehaviorBatch } from '../../src/interaction/behaviorQueue'
import type { BehaviorBatch } from '../../src/interaction/behaviorUploader'

function batch(batchId = 'batch-1'): BehaviorBatch {
  return { schemaVersion: 1, batchId, streamId: 'stream-1', sequence: 1, events: [{
    type: 'click', timestamp: 1_700_000_000_000, sessionElapsedMs: 100,
    pageId: 'feed', x: 12, y: 24, componentId: 'post:like', componentType: 'PostLike',
  }] }
}

beforeEach(() => {
  vi.stubGlobal('indexedDB', new IDBFactory())
  vi.stubGlobal('IDBKeyRange', IDBKeyRange)
})
afterEach(() => vi.unstubAllGlobals())

describe('Persistent behavior queue', () => {
  it('loads the original batch after the queue module is recreated', async () => {
    const original = batch()
    await savePendingBehaviorBatch('session-A', original)
    vi.resetModules()
    const restored = await import('../../src/interaction/behaviorQueue')
    expect((await restored.loadPendingBehaviorBatches('session-A')).map(record => record.batch)).toEqual([original])
  })

  it('keeps sessions isolated', async () => {
    await savePendingBehaviorBatch('session-A', batch('A'))
    await savePendingBehaviorBatch('session-B', batch('B'))
    expect((await loadPendingBehaviorBatches('session-A')).map(record => record.id)).toEqual(['A'])
    expect((await loadPendingBehaviorBatches('session-B')).map(record => record.id)).toEqual(['B'])
    expect(await loadPendingBehaviorBatches('unknown')).toEqual([])
  })

  it('loads batches in creation order rather than identifier order', async () => {
    const date = vi.spyOn(Date, 'now')
    date.mockReturnValue(100)
    try {
      await savePendingBehaviorBatch('session-A', batch('z-first'))
      date.mockReturnValue(200)
      await savePendingBehaviorBatch('session-A', batch('a-second'))
      expect((await loadPendingBehaviorBatches('session-A')).map(record => record.id)).toEqual(['z-first', 'a-second'])
    } finally { date.mockRestore() }
  })

  it('stores only one record for a repeated batch id', async () => {
    await savePendingBehaviorBatch('session-A', batch())
    await savePendingBehaviorBatch('session-A', batch())
    expect(await loadPendingBehaviorBatches('session-A')).toHaveLength(1)
  })

  it('deletes only the acknowledged batch and tolerates repeated deletion', async () => {
    await savePendingBehaviorBatch('session-A', batch('first'))
    await savePendingBehaviorBatch('session-A', batch('second'))
    await deletePendingBehaviorBatch('first')
    await deletePendingBehaviorBatch('first')
    expect((await loadPendingBehaviorBatches('session-A')).map(record => record.id)).toEqual(['second'])
  })
})
