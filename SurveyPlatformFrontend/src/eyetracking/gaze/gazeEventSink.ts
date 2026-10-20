import type { GazeEvent, GazeEventBatch } from './types'

/**
 * The seam to the shared event pipeline (FR-48). The implementation must durably enqueue the batch
 * keyed by (sessionId, sequence) and resolve only once it has accepted it. A rejected batch is not
 * re-queued here: retry, backfill and idempotency belong to the transport owner, and this module
 * counts the failure instead of hiding it.
 */
export interface GazeEventSink {
  deliver(batch: GazeEventBatch): void | Promise<void>
}

export interface GazeBatchCollector extends GazeEventSink {
  readonly batches: readonly GazeEventBatch[]
  readonly events: readonly GazeEvent[]
  readonly droppedEventsTotal: number
}

/**
 * In-memory development sink. It enforces the two invariants the real transport depends on — one
 * session, gap-free sequence — and has no persistence, retry or refresh recovery.
 */
export function collectGazeBatches(sessionId: string): GazeBatchCollector {
  const batches: GazeEventBatch[] = []
  return {
    deliver(batch: GazeEventBatch): void {
      if (batch.sessionId !== sessionId) throw new Error('Gaze batch session mismatch.')
      const previous = batches.at(-1)
      if (previous && batch.sequence !== previous.sequence + 1) {
        throw new Error(
          `Gaze batch sequence gap: expected ${previous.sequence + 1}, received ${batch.sequence}.`,
        )
      }
      batches.push(batch)
    },
    get batches(): readonly GazeEventBatch[] {
      return Object.freeze([...batches])
    },
    get events(): readonly GazeEvent[] {
      return Object.freeze(batches.flatMap((batch) => JSON.parse(batch.payload) as GazeEvent[]))
    },
    get droppedEventsTotal(): number {
      return batches.at(-1)?.droppedEventsTotal ?? 0
    },
  }
}
