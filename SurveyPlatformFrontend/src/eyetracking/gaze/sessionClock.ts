export interface SessionClockReading {
  /** Raw reading on the monotonic clock, as GazeSample.timestamp reports it. */
  readonly monotonicMs: number
  /** Monotonic milliseconds since the clock origin. */
  readonly sessionElapsedMs: number
  readonly wallMs: number
  readonly iso: string
}

/**
 * The single time axis for one participant session.
 *
 * Wall-clock times are derived from the monotonic clock rather than read from Date.now() at the
 * moment of the event, so a mid-session NTP correction cannot reorder two events. Behavioural
 * collection (FR-47) must share this same instance, otherwise the two streams cannot be aligned.
 */
export interface SessionClock {
  readonly originWallMs: number
  readonly originMonotonicMs: number
  readonly originIso: string
  elapsedFrom(monotonicMs: number): number
  wallMsFrom(monotonicMs: number): number
  isoFrom(monotonicMs: number): string
  readingFrom(monotonicMs: number): SessionClockReading
  now(): SessionClockReading
}

export interface SessionClockOptions {
  /** Monotonic source; must be the same one the gaze engine stamps samples with. */
  now?: () => number
  wallClock?: () => number
}

/** Captures one (wall, monotonic) pair at construction. Create it once, when the session starts. */
export function createSessionClock(options: SessionClockOptions = {}): SessionClock {
  const monotonic = options.now ?? (() => performance.now())
  const wall = options.wallClock ?? (() => Date.now())
  const originMonotonicMs = monotonic()
  const originWallMs = wall()
  if (!Number.isFinite(originMonotonicMs)) {
    throw new RangeError('The session clock needs a finite monotonic origin.')
  }
  requireDate(originWallMs)

  const elapsedFrom = (monotonicMs: number): number => {
    if (!Number.isFinite(monotonicMs)) {
      throw new RangeError('A session clock reading must be a finite monotonic value.')
    }
    // Not clamped at zero: a reading that predates the origin stays negative, because collapsing
    // several such readings onto the origin would destroy their order.
    return monotonicMs - originMonotonicMs
  }
  const wallMsFrom = (monotonicMs: number): number => {
    const wallMs = originWallMs + elapsedFrom(monotonicMs)
    return requireDate(wallMs)
  }
  const isoFrom = (monotonicMs: number): string => new Date(wallMsFrom(monotonicMs)).toISOString()
  const readingFrom = (monotonicMs: number): SessionClockReading =>
    Object.freeze({
      monotonicMs,
      sessionElapsedMs: elapsedFrom(monotonicMs),
      wallMs: wallMsFrom(monotonicMs),
      iso: isoFrom(monotonicMs),
    })

  return Object.freeze({
    originWallMs,
    originMonotonicMs,
    originIso: new Date(originWallMs).toISOString(),
    elapsedFrom,
    wallMsFrom,
    isoFrom,
    readingFrom,
    now: () => readingFrom(monotonic()),
  })
}

function requireDate(wallMs: number): number {
  if (!Number.isFinite(wallMs) || Number.isNaN(new Date(wallMs).getTime())) {
    throw new RangeError('The session clock produced a wall-clock time that is not a valid date.')
  }
  return wallMs
}
