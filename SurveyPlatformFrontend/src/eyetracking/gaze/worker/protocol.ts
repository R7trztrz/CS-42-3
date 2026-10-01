import type { GazeEventBufferOptions } from '../gazeEventBuffer'
import type { GazeBufferStats, GazeEvent, GazeEventBatch } from '../types'

/** Options that survive structured cloning; function options stay on the side that owns them. */
export type GazeBufferWorkerOptions = Omit<GazeEventBufferOptions, 'createBatchId'>

export type GazeBufferCommand =
  | { readonly type: 'init'; readonly options: GazeBufferWorkerOptions }
  | { readonly type: 'add'; readonly event: GazeEvent }
  | { readonly type: 'flush' }
  | { readonly type: 'release' }
  | { readonly type: 'drain' }

export type GazeBufferMessage =
  | { readonly type: 'batch'; readonly batch: GazeEventBatch }
  | { readonly type: 'drained' }
  | { readonly type: 'stats'; readonly stats: GazeBufferStats }
  | { readonly type: 'failed'; readonly message: string }

/** The subset of the worker global this module uses, so no worker lib typings are needed. */
export interface GazeWorkerScope {
  postMessage(message: GazeBufferMessage): void
  addEventListener(type: 'message', listener: (event: { data: GazeBufferCommand }) => void): void
}
