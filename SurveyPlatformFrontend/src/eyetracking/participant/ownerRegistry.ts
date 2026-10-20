import type { ParticipantEyeTracking } from './participantEyeTracking'

export interface ParticipantEyeTrackingLease {
  readonly owner: ParticipantEyeTracking
  /** Idempotent. The owner is disposed once no lease remains after the current task. */
  release(): void
}

interface Entry {
  readonly owner: ParticipantEyeTracking
  leases: number
  disposeTimer: ReturnType<typeof setTimeout> | null
}

const entries = new Map<string, Entry>()
let pageHideHandled = false

/**
 * Returns the page's eye-tracking owner for a participant session, creating it on first use.
 *
 * Disposal is deferred by one task after the last lease is released, so React StrictMode's
 * unmount/remount of an effect keeps the same owner, engine and camera. WebGazer is a page-wide
 * singleton, so creating an owner for another session first disposes the existing one, and
 * leaving the page (pagehide) disposes everything.
 */
export function leaseParticipantEyeTracking(
  key: string,
  create: () => ParticipantEyeTracking,
): ParticipantEyeTrackingLease {
  handlePageHide()
  let entry = entries.get(key)
  if (entry?.owner.disposed) {
    disposeEntry(key, entry)
    entry = undefined
  }
  if (!entry) {
    for (const [otherKey, other] of [...entries]) disposeEntry(otherKey, other)
    entry = { owner: create(), leases: 0, disposeTimer: null }
    entries.set(key, entry)
  }
  if (entry.disposeTimer !== null) {
    clearTimeout(entry.disposeTimer)
    entry.disposeTimer = null
  }
  entry.leases += 1
  const leased = entry
  let released = false
  return {
    owner: leased.owner,
    release() {
      if (released) return
      released = true
      leased.leases -= 1
      if (leased.leases > 0 || entries.get(key) !== leased) return
      leased.disposeTimer = setTimeout(() => {
        leased.disposeTimer = null
        if (leased.leases === 0) disposeEntry(key, leased)
      }, 0)
    },
  }
}

/** Disposes every owner immediately; registered for pagehide. */
export function disposeAllParticipantEyeTracking(): void {
  for (const [key, entry] of [...entries]) disposeEntry(key, entry)
}

function disposeEntry(key: string, entry: Entry): void {
  if (entries.get(key) === entry) entries.delete(key)
  if (entry.disposeTimer !== null) {
    clearTimeout(entry.disposeTimer)
    entry.disposeTimer = null
  }
  entry.owner.dispose()
}

function handlePageHide(): void {
  if (pageHideHandled || typeof window === 'undefined') return
  pageHideHandled = true
  window.addEventListener('pagehide', disposeAllParticipantEyeTracking)
}
