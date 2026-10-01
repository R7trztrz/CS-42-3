import { createGazeBufferHost, type GazeBufferHost } from '../gazeEventBuffer'
import type { GazeBufferMode, GazeBufferStats, GazeEvent, GazeEventBatch } from '../types'
import type { GazeBufferCommand, GazeBufferMessage, GazeBufferWorkerOptions } from './protocol'

export interface GazeBufferPortHandlers {
  onBatch(batch: GazeEventBatch): void
  onDrained(): void
  /** Queue depth and cumulative overflow, on the flush cadence. */
  onStats(stats: GazeBufferStats): void
  onIssue(error: unknown): void
  /** Events that can no longer be delivered, so the collector can count them instead of guessing. */
  onDiscard(count: number): void
}

export interface GazeBufferPort {
  readonly mode: GazeBufferMode
  add(event: GazeEvent): void
  flush(): void
  release(): void
  drain(): void
  close(): void
}

export type GazeBufferPortFactory = (
  options: GazeBufferWorkerOptions,
  handlers: GazeBufferPortHandlers,
) => GazeBufferPort

/**
 * Runs the shared buffer host on the main thread, emitting batches on a microtask so the caller
 * never re-enters the sample handler. This is the documented fallback for environments where a
 * module worker cannot be created.
 */
export const createMainThreadBufferPort: GazeBufferPortFactory = (options, handlers) => {
  let closed = false
  const host = createGazeBufferHost(options, {
    batch: (batch) => queueMicrotask(() => !closed && handlers.onBatch(batch)),
    drained: () => queueMicrotask(() => !closed && handlers.onDrained()),
    stats: (stats) => queueMicrotask(() => !closed && handlers.onStats(stats)),
  })
  const guard = (action: () => void): void => {
    if (closed) return
    try {
      action()
    } catch (error) {
      handlers.onIssue(error)
    }
  }
  return {
    mode: 'MAIN_THREAD',
    add: (event) => guard(() => host.add(event)),
    flush: () => guard(() => host.flush()),
    release: () => guard(() => host.release()),
    drain: () => guard(() => host.drain()),
    close: () => {
      closed = true
    },
  }
}

/** Creates the module worker. Throws when workers are unavailable; the caller falls back. */
export const createWorkerBufferPort: GazeBufferPortFactory = (options, handlers) => {
  if (typeof Worker === 'undefined') throw new Error('Web Workers are not available.')
  const worker = new Worker(new URL('./bufferWorker.ts', import.meta.url), { type: 'module' })
  let closed = false
  let failed = false
  let fallback: GazeBufferHost | null = null
  let posted = 0
  let returned = 0
  let lastSequence = 0
  let droppedTotal = 0

  const failover = (error: unknown): void => {
    if (failed || closed) return
    failed = true
    handlers.onIssue(error)
    worker.terminate()
    // Reported separately from the buffer's own overflow total, which stays cumulative, so the
    // collector can add the two without counting the same event twice.
    const lost = Math.max(0, posted - returned - droppedTotal)
    if (lost > 0) handlers.onDiscard(lost)
    // Numbering continues from the last batch the worker produced, so the session's batch
    // sequence stays gap-free across the failover.
    fallback = createGazeBufferHost(
      { ...options, initialSequence: lastSequence, initialDroppedTotal: droppedTotal },
      {
        batch: (batch) => queueMicrotask(() => !closed && handlers.onBatch(batch)),
        drained: () => queueMicrotask(() => !closed && handlers.onDrained()),
        stats: (stats) => queueMicrotask(() => !closed && handlers.onStats(stats)),
      },
    )
  }

  worker.addEventListener('message', (event: MessageEvent<GazeBufferMessage>) => {
    if (closed) return
    const message = event.data
    if (message.type === 'batch') {
      returned += message.batch.eventCount
      lastSequence = message.batch.sequence
      droppedTotal = message.batch.droppedEventsTotal
      handlers.onBatch(message.batch)
      return
    }
    if (message.type === 'drained') {
      handlers.onDrained()
      return
    }
    if (message.type === 'stats') {
      // Keeps the failover accounting accurate even while no batch is getting through.
      droppedTotal = message.stats.droppedEventsTotal
      handlers.onStats(message.stats)
      return
    }
    failover(new Error(`The gaze buffer worker failed: ${message.message}`))
  })
  worker.addEventListener('error', (event) => failover(event.error ?? new Error(event.message)))
  worker.addEventListener('messageerror', () =>
    failover(new Error('The gaze buffer worker received an uncloneable message.')),
  )
  worker.postMessage({ type: 'init', options })

  const send = (command: GazeBufferCommand, onFallback: () => void): void => {
    if (closed) return
    if (fallback) {
      try {
        onFallback()
      } catch (error) {
        handlers.onIssue(error)
      }
      return
    }
    try {
      worker.postMessage(command)
    } catch (error) {
      failover(error)
      if (fallback) onFallback()
    }
  }

  return {
    // Reports the path actually in use, so a failover is visible rather than assumed.
    get mode(): GazeBufferMode {
      return fallback ? 'MAIN_THREAD' : 'WORKER'
    },
    add: (event) => {
      posted += 1
      send({ type: 'add', event }, () => fallback?.add(event))
    },
    flush: () => send({ type: 'flush' }, () => fallback?.flush()),
    release: () => send({ type: 'release' }, () => fallback?.release()),
    drain: () => send({ type: 'drain' }, () => fallback?.drain()),
    close: () => {
      closed = true
      worker.terminate()
    },
  }
}

/** Prefers the worker and degrades to the main thread, which is what the NFR asks for. */
export const createGazeBufferPort: GazeBufferPortFactory = (options, handlers) => {
  try {
    return createWorkerBufferPort(options, handlers)
  } catch (error) {
    handlers.onIssue(error)
    return createMainThreadBufferPort(options, handlers)
  }
}
