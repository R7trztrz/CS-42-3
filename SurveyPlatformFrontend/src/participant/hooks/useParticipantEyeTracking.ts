// M6: React access to the page's eye-tracking owner for one participant
// session. The owner (camera, calibration, gaze collection) lives outside
// React; this hook only leases it and subscribes to its snapshots.

import { useCallback, useEffect, useState, useSyncExternalStore } from 'react'

import {
  createHttpEyeTrackingTransport,
  EYE_TRACKING_TEST_MODE,
  leaseParticipantEyeTracking,
  ParticipantEyeTracking,
  type ParticipantEyeTrackingIssue,
  type ParticipantEyeTrackingSnapshot,
} from '../../eyetracking/participant'

function reportIssue(issue: ParticipantEyeTrackingIssue) {
  // Operational diagnostics only. Event loss is counted on the gaze batches
  // themselves; nothing here may interrupt the participant.
  console.warn('[eye-tracking]', issue.stage, issue.error)
}

// Returns the owner while `active` is true and the session is known. Leasing
// is keyed by session id, so StrictMode remounts and re-renders keep the same
// camera; when `active` turns false the owner is disposed and the camera
// released.
export function useParticipantEyeTracking(
  sessionId: string | null,
  sessionToken: string | null,
  active: boolean,
): ParticipantEyeTracking | null {
  const [owner, setOwner] = useState<ParticipantEyeTracking | null>(null)

  useEffect(() => {
    if (!active || !sessionId || !sessionToken) {
      setOwner(null)
      return
    }

    const lease = leaseParticipantEyeTracking(
      sessionId,
      () =>
        new ParticipantEyeTracking({
          sessionId,
          transport: createHttpEyeTrackingTransport(sessionToken),
          showGazeDotWhileBrowsing: EYE_TRACKING_TEST_MODE,
          onIssue: reportIssue,
        }),
    )
    setOwner(lease.owner)

    return () => lease.release()
  }, [active, sessionId, sessionToken])

  return owner
}

const noSubscription = () => () => {}

export function useEyeTrackingSnapshot(
  owner: ParticipantEyeTracking | null,
): ParticipantEyeTrackingSnapshot | null {
  const subscribe = useCallback(
    (listener: () => void) => (owner ? owner.subscribe(listener) : noSubscription()),
    [owner],
  )
  const getSnapshot = useCallback(() => owner?.getSnapshot() ?? null, [owner])

  return useSyncExternalStore(subscribe, getSnapshot, getSnapshot)
}
