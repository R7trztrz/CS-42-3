import { useEffect } from 'react'
import { CalibrationView } from '../../pages/participant/ParticipantDemoTemplate'

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
    <CalibrationView
      onNext={() => void onRefreshSession()}
      nextLabel="Check calibration"
      previewStatus="Waiting"
      previewCaption="Camera is not active"
      notice={
        <div className="demo-notice">
          <div>
            <strong>Waiting for calibration to be confirmed</strong>
            <p role="status">Calibration is not available in this build. This page will continue once calibration is confirmed.</p>
          </div>
        </div>
      }
    />
  )
}

export default CalibrationStep
