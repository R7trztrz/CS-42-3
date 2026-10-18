import { createGazeBufferHost, type GazeBufferHost } from '../gazeEventBuffer'
import type { GazeWorkerScope } from './protocol'

/**
 * Worker entry. It owns buffering, batching and serialization only; it never fetches, so retry and
 * backfill stay with the transport owner (FR-48) on the main thread.
 */
const scope = globalThis as unknown as GazeWorkerScope
let host: GazeBufferHost | null = null

scope.addEventListener('message', (event) => {
  const command = event.data
  try {
    switch (command.type) {
      case 'init':
        host = createGazeBufferHost(command.options, {
          batch: (batch) => scope.postMessage({ type: 'batch', batch }),
          drained: () => scope.postMessage({ type: 'drained' }),
          stats: (stats) => scope.postMessage({ type: 'stats', stats }),
        })
        break
      case 'add':
        host?.add(command.event)
        break
      case 'flush':
        host?.flush()
        break
      case 'release':
        host?.release()
        break
      case 'drain':
        host?.drain()
        break
    }
  } catch (error) {
    // The main thread fails over rather than losing the rest of the session in silence.
    scope.postMessage({
      type: 'failed',
      message: error instanceof Error ? error.message : String(error),
    })
  }
})
