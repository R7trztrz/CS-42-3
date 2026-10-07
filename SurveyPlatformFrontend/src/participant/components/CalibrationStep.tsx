import { useEffect, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

import type {
  CalibrationAttempt,
  CalibrationProgress,
  CalibrationTarget,
  EyeTrackingUnavailableReason,
} from '../../eyetracking'
import {
  CALIBRATION_TARGET_SETTLE_MS,
  type ParticipantEyeTracking,
  type ParticipantEyeTrackingSnapshot,
} from '../../eyetracking/participant'
import { useEyeTrackingSnapshot } from '../hooks/useParticipantEyeTracking'
import './eyeTracking.css'

// M6: the CALIBRATION phase. Runs FR-49 calibration through the session's
// eye-tracking owner, then completes the phase on the server with the attempt
// the participant continues with. Every outcome lets the participant continue:
// a poor result, a skipped calibration and an unavailable camera included.

type CalibrationStepProps = {
  eyeTracking: ParticipantEyeTracking | null
  isCompleting: boolean
  errorMessage: string | null
  onComplete: (attempt: CalibrationAttempt) => Promise<boolean>
}

const PRIMARY_BUTTON =
  'rounded-sm bg-emerald-700 px-5 py-2.5 text-sm font-semibold text-white hover:bg-emerald-800 disabled:opacity-60'
const SECONDARY_BUTTON =
  'rounded-sm border border-gray-300 bg-white px-5 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-60'

const UNAVAILABLE_REASONS: Record<EyeTrackingUnavailableReason, string> = {
  NO_CAMERA_API: 'This browser does not allow camera access on this page.',
  NO_CAMERA_DEVICE: 'No camera was found.',
  PERMISSION_DENIED: 'Camera access was blocked.',
  INIT_FAILED: 'The eye tracker could not be started.',
  INIT_CANCELLED: 'Calibration was skipped before the camera started.',
  INIT_TIMEOUT: 'The camera took too long to start.',
}

function Card({ children }: { children: ReactNode }) {
  return (
    <section className="mb-6 rounded-md border border-gray-200 bg-white px-6 py-6 shadow-sm">
      <p className="text-xs font-semibold uppercase text-emerald-700">Eye-tracking calibration</p>
      {children}
    </section>
  )
}

function CalibrationStep({ eyeTracking, isCompleting, errorMessage, onComplete }: CalibrationStepProps) {
  const snapshot = useEyeTrackingSnapshot(eyeTracking)

  if (!eyeTracking || !snapshot) {
    return (
      <Card>
        <p className="mt-2 text-sm text-gray-600" role="status">
          Preparing eye tracking...
        </p>
      </Card>
    )
  }

  switch (snapshot.stage) {
    case 'IDLE':
      return (
        <Card>
          <h2 className="mt-1 text-xl font-semibold text-[#172033]">Calibrate the eye tracker</h2>
          <p className="mt-3 max-w-3xl text-sm leading-6 text-gray-700">
            This study uses your webcam to estimate where you look on the screen. The video is
            processed in your browser and is never uploaded; only the estimated screen positions
            are saved with your responses.
          </p>
          <ul className="mt-3 max-w-3xl list-disc space-y-1 pl-5 text-sm leading-6 text-gray-700">
            <li>Sit about an arm&apos;s length from the screen with your face evenly lit.</li>
            <li>Keep your head still and follow the dots with your eyes.</li>
            <li>Nine dots appear one at a time. Look at each dot and click it five times.</li>
          </ul>
          <p className="mt-3 text-sm text-gray-600">
            Your browser will ask for camera access. If the camera is unavailable, you can still
            continue the study.
          </p>
          <div className="mt-5">
            <button type="button" className={PRIMARY_BUTTON} onClick={() => void eyeTracking.calibrate()}>
              Start calibration
            </button>
          </div>
        </Card>
      )
    case 'STARTING':
      return (
        <Card>
          <h2 className="mt-1 text-xl font-semibold text-[#172033]">Starting the camera...</h2>
          <p className="mt-3 text-sm text-gray-700" role="status">
            Allow camera access if your browser asks. This can take a few seconds.
          </p>
          <div className="mt-5">
            <button type="button" className={SECONDARY_BUTTON} onClick={() => eyeTracking.skip()}>
              Skip calibration
            </button>
          </div>
        </Card>
      )
    case 'CALIBRATING':
      return (
        <CalibrationOverlay
          eyeTracking={eyeTracking}
          target={snapshot.target}
          progress={snapshot.progress}
        />
      )
    case 'FINISHED':
      return snapshot.attempt ? (
        <CalibrationResult
          eyeTracking={eyeTracking}
          snapshot={snapshot}
          attempt={snapshot.attempt}
          isCompleting={isCompleting}
          errorMessage={errorMessage}
          onComplete={onComplete}
        />
      ) : null
    default:
      return (
        <Card>
          <p className="mt-2 text-sm text-gray-600" role="status">
            Continuing to the study...
          </p>
        </Card>
      )
  }
}

type CalibrationOverlayProps = {
  eyeTracking: ParticipantEyeTracking
  target: CalibrationTarget | null
  progress: CalibrationProgress | null
}

function CalibrationOverlay({ eyeTracking, target, progress }: CalibrationOverlayProps) {
  // The controller ignores clicks for a short settle window after each target
  // appears. The dot is disabled for the same window so a fast click is not
  // silently dropped.
  const [armedTarget, setArmedTarget] = useState<CalibrationTarget | null>(null)

  useEffect(() => {
    if (!target) return
    const timer = window.setTimeout(() => setArmedTarget(target), CALIBRATION_TARGET_SETTLE_MS)
    return () => window.clearTimeout(timer)
  }, [target])

  useEffect(() => {
    const onResize = () => eyeTracking.checkViewport()
    window.addEventListener('resize', onResize)
    return () => window.removeEventListener('resize', onResize)
  }, [eyeTracking])

  const armed = target !== null && armedTarget === target
  const clicksRecorded =
    progress &&
    target &&
    progress.target.kind === target.kind &&
    progress.target.index === target.index
      ? progress.clicksRecorded
      : 0
  const clicksLeft = target ? target.clicksRequired - clicksRecorded : 0
  const total = progress?.targetsTotal ?? 9
  const current = target ? Math.min(target.index + 1, total) : 0

  return createPortal(
    <div className="et-calibration-overlay" role="dialog" aria-modal="true" aria-label="Eye-tracking calibration">
      <div className="et-calibration-backdrop" />
      <div className="et-calibration-help" role="status">
        <span className="et-calibration-progress">
          Point {current} of {total}
        </span>
        <span>
          {armed
            ? `Look at the red dot and click it ${clicksLeft} more ${clicksLeft === 1 ? 'time' : 'times'}.`
            : 'Look at the red dot...'}
        </span>
      </div>
      <button type="button" className="et-calibration-skip" onClick={() => eyeTracking.skip()}>
        Skip calibration
      </button>
      {target && (
        <button
          type="button"
          className="et-calibration-target"
          aria-label="Calibration point"
          disabled={!armed}
          style={{ left: `${target.targetX}px`, top: `${target.targetY}px` }}
          onClick={() => eyeTracking.registerClick()}
        />
      )}
    </div>,
    document.body,
  )
}

type CalibrationResultProps = {
  eyeTracking: ParticipantEyeTracking
  snapshot: ParticipantEyeTrackingSnapshot
  attempt: CalibrationAttempt
  isCompleting: boolean
  errorMessage: string | null
  onComplete: (attempt: CalibrationAttempt) => Promise<boolean>
}

function resultText(attempt: CalibrationAttempt): { title: string; detail: string } {
  if (!attempt.trackingAvailable) {
    const reason = attempt.unavailableReason
      ? UNAVAILABLE_REASONS[attempt.unavailableReason]
      : UNAVAILABLE_REASONS.INIT_FAILED
    return {
      title: 'Eye tracking is not available',
      detail: `${reason} You can continue the study without eye tracking.`,
    }
  }
  if (attempt.outcome === 'ABANDONED') {
    return {
      title: 'Calibration was not completed',
      detail: 'Eye tracking will continue, but its accuracy will be lower.',
    }
  }
  switch (attempt.quality) {
    case 'GOOD':
      return { title: 'Calibration complete', detail: 'Accuracy looks good.' }
    case 'FAIR':
      return { title: 'Calibration complete', detail: 'Accuracy is acceptable.' }
    case 'POOR':
      return { title: 'Calibration complete', detail: 'Accuracy is low.' }
    default:
      return { title: 'Calibration complete', detail: 'Its accuracy could not be measured.' }
  }
}

function CalibrationResult({
  eyeTracking,
  snapshot,
  attempt,
  isCompleting,
  errorMessage,
  onComplete,
}: CalibrationResultProps) {
  const { title, detail } = resultText(attempt)

  return (
    <Card>
      <h2 className="mt-1 text-xl font-semibold text-[#172033]">{title}</h2>
      <p className="mt-3 text-sm text-gray-700">{detail}</p>
      {snapshot.canRecalibrate && (
        <p className="mt-2 text-sm text-gray-600">
          You can recalibrate to improve accuracy, or continue now.
        </p>
      )}
      {errorMessage && (
        <p className="mt-3 text-sm font-semibold text-red-700" role="alert">
          {errorMessage}
        </p>
      )}
      <div className="mt-5 flex flex-wrap gap-3">
        <button
          type="button"
          className={PRIMARY_BUTTON}
          disabled={isCompleting}
          onClick={() => void onComplete(attempt)}
        >
          {isCompleting ? 'Saving...' : 'Continue to the study'}
        </button>
        {snapshot.canRecalibrate && (
          <button
            type="button"
            className={SECONDARY_BUTTON}
            disabled={isCompleting}
            onClick={() => void eyeTracking.calibrate()}
          >
            Recalibrate
          </button>
        )}
      </div>
    </Card>
  )
}

export default CalibrationStep
