import type { GazeBufferStats, GazeEvent, GazeEventBatch } from './types'

export const DEFAULT_MAX_BUFFERED_EVENTS = 5000
export const DEFAULT_BATCH_SIZE = 200
const MAX_BUFFERED_EVENTS_LIMIT = 1_000_000

export interface GazeEventBufferOptions {
  readonly sessionId: string
  /** Upper bound on events held here, not on events already handed to the transport owner. */
  readonly maxEvents?: number
  readonly batchSize?: number
  /** Continues numbering after a failover, so the session's sequence stays gap-free. */
  readonly initialSequence?: number
  readonly initialDroppedTotal?: number
  readonly createBatchId?: () => string
}

/**
 * Holds events, cuts batches and serializes them. No timers, no DOM and no transport, so the same
 * instance behaves identically inside a worker and on the main thread.
 *
 * Only one batch is outstanding at a time. Events keep accumulating here while the transport owner
 * works on it, which is what makes the capacity limit meaningful: everything not yet accepted is
 * still in this queue. Once the limit is reached the oldest event is discarded and counted; nothing
 * is lost silently.
 */
export class GazeEventBuffer {
  private readonly sessionId: string
  private readonly maxEvents: number
  private readonly batchSizeValue: number
  private readonly createBatchId: (() => string) | undefined
  private readonly pending: GazeEvent[] = []
  private sequence: number
  private outstanding = false
  private droppedSinceLastBatch = 0
  private droppedTotal: number

  constructor(options: GazeEventBufferOptions) {
    if (!options.sessionId.trim()) throw new Error('A participant session ID is required.')
    this.sessionId = options.sessionId
    this.maxEvents = integer(
      options.maxEvents ?? DEFAULT_MAX_BUFFERED_EVENTS,
      'maxEvents',
      1,
      MAX_BUFFERED_EVENTS_LIMIT,
    )
    this.batchSizeValue = integer(
      options.batchSize ?? Math.min(DEFAULT_BATCH_SIZE, this.maxEvents),
      'batchSize',
      1,
      this.maxEvents,
    )
    this.sequence = integer(options.initialSequence ?? 0, 'initialSequence', 0, Number.MAX_SAFE_INTEGER)
    this.droppedTotal = integer(
      options.initialDroppedTotal ?? 0,
      'initialDroppedTotal',
      0,
      Number.MAX_SAFE_INTEGER,
    )
    this.createBatchId = options.createBatchId
  }

  get pendingCount(): number {
    return this.pending.length
  }
  get batchSize(): number {
    return this.batchSizeValue
  }
  get capacity(): number {
    return this.maxEvents
  }
  get droppedEventsTotal(): number {
    return this.droppedTotal
  }
  get lastSequence(): number {
    return this.sequence
  }
  get hasOutstandingBatch(): boolean {
    return this.outstanding
  }
  /** Nothing left to send and nothing awaiting acknowledgement. */
  get isIdle(): boolean {
    return this.pending.length === 0 && !this.outstanding
  }

  /** Returns a batch only when one can be cut right now. */
  add(event: GazeEvent): GazeEventBatch | null {
    this.pending.push(event)
    while (this.pending.length > this.maxEvents) {
      this.pending.shift()
      this.droppedSinceLastBatch += 1
      this.droppedTotal += 1
    }
    return this.canCut() && this.pending.length >= this.batchSizeValue ? this.cut() : null
  }

  /** Cuts whatever is queued, however small. Used by the flush cadence and by draining. */
  flush(): GazeEventBatch | null {
    return this.canCut() && this.pending.length > 0 ? this.cut() : null
  }

  /** The transport owner has settled the outstanding batch. */
  release(): GazeEventBatch | null {
    this.outstanding = false
    return this.pending.length >= this.batchSizeValue ? this.cut() : null
  }

  private canCut(): boolean {
    return !this.outstanding
  }

  private cut(): GazeEventBatch {
    const events = this.pending.splice(0, this.batchSizeValue)
    this.sequence += 1
    const droppedEvents = this.droppedSinceLastBatch
    this.droppedSinceLastBatch = 0
    this.outstanding = true
    return Object.freeze({
      schemaVersion: 1 as const,
      sessionId: this.sessionId,
      batchId: this.nextBatchId(),
      sequence: this.sequence,
      eventCount: events.length,
      droppedEvents,
      droppedEventsTotal: this.droppedTotal,
      payload: JSON.stringify(events),
    })
  }

  private nextBatchId(): string {
    const supplied = this.createBatchId?.()
    if (supplied?.trim()) return supplied
    const uuid = globalThis.crypto?.randomUUID?.bind(globalThis.crypto)
    return uuid ? uuid() : `${this.sessionId}-batch-${this.sequence}`
  }
}

export interface GazeBufferHostEvents {
  batch(batch: GazeEventBatch): void
  drained(): void
  /** Queue depth and cumulative overflow, reported on every flush, cut or not. */
  stats(stats: GazeBufferStats): void
}

export interface GazeBufferHost {
  add(event: GazeEvent): void
  flush(): void
  release(): void
  drain(): void
  readonly buffer: GazeEventBuffer
}

/**
 * The one state machine both buffering paths run. Keeping it here rather than writing it twice is
 * what guarantees the worker and the main-thread fallback emit the same batches for the same input.
 */
export function createGazeBufferHost(
  options: GazeEventBufferOptions,
  events: GazeBufferHostEvents,
): GazeBufferHost {
  const buffer = new GazeEventBuffer(options)
  let draining = false
  const push = (batch: GazeEventBatch | null): boolean => {
    if (!batch) return false
    events.batch(batch)
    return true
  }
  const settle = (): void => {
    if (draining && buffer.isIdle) {
      draining = false
      events.drained()
    }
  }
  /**
   * Reported whenever anything moves except a plain enqueue. That keeps the message rate at the
   * flush cadence plus one per batch, while making a queue that grows behind a stuck transport
   * visible immediately instead of only when a later batch gets through.
   */
  const report = (): void => {
    events.stats(
      Object.freeze({
        pendingCount: buffer.pendingCount,
        droppedEventsTotal: buffer.droppedEventsTotal,
      }),
    )
  }
  return {
    add: (event) => {
      push(buffer.add(event))
    },
    flush: () => {
      if (!push(buffer.flush())) settle()
      report()
    },
    release: () => {
      if (!push(buffer.release()) && draining && !push(buffer.flush())) settle()
      report()
    },
    drain: () => {
      draining = true
      if (!push(buffer.flush())) settle()
      report()
    },
    buffer,
  }
}

function integer(value: number, name: string, min: number, max: number): number {
  if (!Number.isInteger(value) || value < min || value > max) {
    throw new RangeError(`${name} must be an integer between ${min} and ${max}.`)
  }
  return value
}
