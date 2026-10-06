import { useEffect } from 'react'

// M6 boundary: there is no public HTTP endpoint for completing
// calibration (see M5前端联调交接说明_2026-10-05.md section 7). Production
// calibration persistence calls
// ParticipantCalibrationLifecyclePort.completeCalibration(sessionId)
// directly against the backend, which then advances this session's phase
// to BROWSING on its own.
//
// This component intentionally does not run the eye-tracking engine or
// claim completion itself -- that would be exactly the kind of frontend
// mock the handoff doc says not to ship. It polls the authoritative
// session so the page moves on automatically once M6 (or a developer
// using the dev no-op path) advances the phase.
const POLL_INTERVAL_MS = 5000

type CalibrationStepProps = {
  onRefreshSession: () => Promise<boolean>
}

function CalibrationStep({ onRefreshSession }: CalibrationStepProps) {
  useEffect(() => {
    const interval = window.setInterval(() => {
      void onRefreshSession()
    }, POLL_INTERVAL_MS)

    return () => window.clearInterval(interval)
  }, [onRefreshSession])

  return (
    <section className="mb-6 rounded-md border border-gray-200 bg-white px-6 py-6 shadow-sm">
      <p className="text-xs font-semibold uppercase text-emerald-700">Eye-tracking calibration</p>
      <h2 className="mt-1 text-lg font-semibold text-[#172033]">
        Waiting for calibration to be confirmed
      </h2>
      <p className="mt-2 text-sm text-gray-600" role="status">
        This step depends on backend calibration persistence (M6), which is not wired up yet in
        this build. The page will continue on its own once the session is confirmed.
      </p>
    </section>
  )
}

export default CalibrationStep
