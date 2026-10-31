import axios from 'axios'
import participantHttpClient from '../participant/api/participantHttpClient'
import {
  getBehaviorEvents,
  takeBehaviorEvents,
  type BehaviorEvent,
} from './behaviorCollector'
import {
  deletePendingBehaviorBatch,
  loadPendingBehaviorBatches,
  savePendingBehaviorBatch,
} from './behaviorQueue'

export interface BehaviorBatch {
  schemaVersion: 1
  batchId: string
  streamId: string
  sequence: number
  events: BehaviorEvent[]
}

interface BehaviorBatchResponse {
  sessionId: string
  batchId: string
  streamId: string
  sequence: number
  eventCount: number
  receivedAt: string
}

export interface BehaviorUploadSessionOptions {
  sessionId: string
  sessionToken: string
  batchSize?: number
  flushIntervalMs?: number
}

const BEHAVIOR_BATCHES_PATH =
  '/api/participant-session/behavior-batches'

const DEFAULT_BATCH_SIZE = 200
const DEFAULT_FLUSH_INTERVAL_MS = 500
const MAX_RETRY_ATTEMPTS = 3

function sleep(ms: number) {
  return new Promise<void>((resolve) => {
    window.setTimeout(resolve, ms)
  })
}

function shouldRetry(error: unknown) {
  if (!axios.isAxiosError(error)) {
    return false
  }

  if (!error.response) {
    return true
  }

  return error.response.status >= 500
}

export class BehaviorUploadSession {
  private readonly sessionId: string
  private readonly sessionToken: string
  private readonly batchSize: number
  private readonly flushIntervalMs: number
  private readonly streamId: string

  private sequence = 1
  private pendingBatches: BehaviorBatch[] = []
  private timer: number | null = null
  private flushPromise: Promise<void> | null = null
  private readonly restorePromise: Promise<void>
  private onlineListenerAttached = false

  constructor(options: BehaviorUploadSessionOptions) {
    const sessionId = options.sessionId.trim()
    const token = options.sessionToken.trim()

    if (!sessionId) {
      throw new Error(
        'Behavior upload requires a participant session id.',
      )
    }

    if (!token) {
      throw new Error(
        'Behavior upload requires a participant session token.',
      )
    }

    this.sessionId = sessionId
    this.sessionToken = token
    this.batchSize =
      options.batchSize ?? DEFAULT_BATCH_SIZE
    this.flushIntervalMs =
      options.flushIntervalMs ??
      DEFAULT_FLUSH_INTERVAL_MS

    this.streamId = crypto.randomUUID()
    this.restorePromise =
      this.restorePendingBatches()
  }

  private readonly handleOnline = () => {
    void this.flush().catch((error) => {
      if (import.meta.env.DEV) {
        console.warn(
          'Behavior upload is still pending after reconnect.',
          error,
        )
      }
    })
  }

  private async restorePendingBatches() {
    try {
      const stored =
        await loadPendingBehaviorBatches(
          this.sessionId,
        )

      const existingIds = new Set(
        this.pendingBatches.map(
          (batch) => batch.batchId,
        ),
      )

      for (const record of stored) {
        if (
          !existingIds.has(
            record.batch.batchId,
          )
        ) {
          this.pendingBatches.push(
            record.batch,
          )
          existingIds.add(
            record.batch.batchId,
          )
        }
      }

      if (
        import.meta.env.DEV &&
        stored.length > 0
      ) {
        console.log(
          'Behavior pending batches restored.',
          {
            count: stored.length,
          },
        )
      }
    } catch (error) {
      console.error(
        'Unable to restore pending behavior batches.',
        error,
      )
    }
  }

  startAutoFlush() {
    if (this.timer !== null) {
      return
    }

    this.timer = window.setInterval(
      () => {
        void this.flush().catch(
          (error) => {
            if (import.meta.env.DEV) {
              console.warn(
                'Behavior batch upload is pending retry.',
                error,
              )
            }
          },
        )
      },
      this.flushIntervalMs,
    )

    if (!this.onlineListenerAttached) {
      window.addEventListener(
        'online',
        this.handleOnline,
      )
      this.onlineListenerAttached = true
    }

    void this.flush().catch(
      (error) => {
        if (import.meta.env.DEV) {
          console.warn(
            'Restored behavior batches remain pending.',
            error,
          )
        }
      },
    )
  }

  stopAutoFlush() {
    if (this.timer !== null) {
      window.clearInterval(
        this.timer,
      )
      this.timer = null
    }

    if (
      this.onlineListenerAttached
    ) {
      window.removeEventListener(
        'online',
        this.handleOnline,
      )
      this.onlineListenerAttached =
        false
    }
  }

  getPendingBatchCount() {
    return this.pendingBatches.length
  }

  getBufferedEventCount() {
    return getBehaviorEvents().length
  }

  async flush() {
    await this.restorePromise

    if (this.flushPromise) {
      return this.flushPromise
    }

    this.flushPromise =
      this.runFlush().finally(() => {
        this.flushPromise = null
      })

    return this.flushPromise
  }

  private async createPendingBatches() {
    while (
      getBehaviorEvents().length > 0
    ) {
      const batchEvents =
        takeBehaviorEvents(
          this.batchSize,
        )

      if (
        batchEvents.length === 0
      ) {
        break
      }

      const batch: BehaviorBatch = {
        schemaVersion: 1,
        batchId:
          crypto.randomUUID(),
        streamId: this.streamId,
        sequence: this.sequence,
        events: batchEvents.map(
          (event) => ({
            ...event,
          }),
        ),
      }

      await savePendingBehaviorBatch(
        this.sessionId,
        batch,
      )

      this.pendingBatches.push(
        batch,
      )

      this.sequence += 1
    }
  }

  private async runFlush() {
    await this.createPendingBatches()

    while (
      this.pendingBatches.length > 0
    ) {
      const batch =
        this.pendingBatches[0]

      await this.sendBatchWithRetry(
        batch,
      )

      await deletePendingBehaviorBatch(
        batch.batchId,
      )

      this.pendingBatches.shift()

      await this.createPendingBatches()
    }
  }

  private async sendBatchWithRetry(
    batch: BehaviorBatch,
  ) {
    let attempt = 0

    while (true) {
      attempt += 1

      try {
        const response =
          await participantHttpClient.post<BehaviorBatchResponse>(
            BEHAVIOR_BATCHES_PATH,
            batch,
            {
              headers: {
                'Content-Type':
                  'application/json',
                'X-Participant-Session-Token':
                  this.sessionToken,
              },
            },
          )

        const acknowledgement =
          response.data

        if (
          acknowledgement.batchId !==
            batch.batchId ||
          acknowledgement.streamId !==
            batch.streamId ||
          acknowledgement.sequence !==
            batch.sequence ||
          acknowledgement.eventCount !==
            batch.events.length
        ) {
          throw new Error(
            'Behavior batch acknowledgement did not match the sent batch.',
          )
        }

        if (import.meta.env.DEV) {
          console.log(
            'Behavior batch uploaded.',
            {
              batchId:
                batch.batchId,
              sequence:
                batch.sequence,
              eventCount:
                batch.events.length,
              status:
                response.status,
            },
          )
        }

        return
      } catch (error) {
        if (
          !shouldRetry(error) ||
          attempt >=
            MAX_RETRY_ATTEMPTS
        ) {
          throw error
        }

        const retryDelay =
          250 *
          2 ** (attempt - 1)

        await sleep(retryDelay)
      }
    }
  }
}

export function createBehaviorUploadSession(
  options: BehaviorUploadSessionOptions,
) {
  return new BehaviorUploadSession(
    options,
  )
}
