import { describe, expect, it } from 'vitest'
import { createSessionClock } from '../../src/eyetracking'

const WALL = 1_700_000_000_000

function clockAt(monotonic: number[], wall: number[]) {
  let monotonicIndex = 0
  let wallIndex = 0
  return createSessionClock({
    now: () => monotonic[Math.min(monotonicIndex++, monotonic.length - 1)],
    wallClock: () => wall[Math.min(wallIndex++, wall.length - 1)],
  })
}

describe('Session clock alignment', () => {
  it('captures one origin pair and derives every later wall time from the monotonic clock', () => {
    const clock = clockAt([500, 1500, 2500], [WALL])
    expect(clock.originMonotonicMs).toBe(500)
    expect(clock.originWallMs).toBe(WALL)
    expect(clock.originIso).toBe(new Date(WALL).toISOString())
    expect(clock.elapsedFrom(1500)).toBe(1000)
    expect(clock.wallMsFrom(1500)).toBe(WALL + 1000)
    expect(clock.isoFrom(1500)).toBe(new Date(WALL + 1000).toISOString())
  })

  it('does not let a wall-clock jump reorder two readings', () => {
    // The wall clock is read once, at construction; a later NTP correction cannot reach the events.
    const clock = clockAt([0], [WALL, WALL - 60_000, WALL + 60_000])
    const earlier = clock.wallMsFrom(10)
    const later = clock.wallMsFrom(20)
    expect(later).toBeGreaterThan(earlier)
    expect(later - earlier).toBe(10)
  })

  it('keeps a reading that predates the origin negative instead of collapsing the order', () => {
    const clock = clockAt([1000], [WALL])
    expect(clock.elapsedFrom(900)).toBe(-100)
    expect(clock.elapsedFrom(950)).toBeGreaterThan(clock.elapsedFrom(900))
    expect(clock.wallMsFrom(900)).toBe(WALL - 100)
  })

  it('reports readings that gaze events can be stamped with directly', () => {
    const clock = clockAt([0, 7], [WALL])
    expect(clock.readingFrom(2500)).toEqual({
      monotonicMs: 2500,
      sessionElapsedMs: 2500,
      wallMs: WALL + 2500,
      iso: new Date(WALL + 2500).toISOString(),
    })
    expect(clock.now()).toMatchObject({ monotonicMs: 7, sessionElapsedMs: 7 })
  })

  it.each([NaN, Infinity, -Infinity])('refuses the non-finite reading %s', (reading) => {
    const clock = clockAt([0], [WALL])
    expect(() => clock.elapsedFrom(reading)).toThrow('finite monotonic value')
    expect(() => clock.isoFrom(reading)).toThrow('finite monotonic value')
  })

  it('refuses an origin or a derived time that is not a valid date', () => {
    expect(() => clockAt([NaN], [WALL])).toThrow('finite monotonic origin')
    expect(() => clockAt([0], [NaN])).toThrow('not a valid date')
    const clock = clockAt([0], [WALL])
    expect(() => clock.isoFrom(1e18)).toThrow('not a valid date')
  })

  it('is frozen so the session cannot be silently re-based mid-run', () => {
    const clock = clockAt([0], [WALL])
    expect(Object.isFrozen(clock)).toBe(true)
  })
})
