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
import { CalibrationView } from '../../pages/participant/ParticipantDemoTemplate'
import { useEyeTrackingSnapshot } from '../hooks/useParticipantEyeTracking'
import './eyeTracking.css'

// M6: the CALIBRATION phase. Runs FR-49 calibration through the session's
// eye-tracking owner, then completes the phase on the server with the attempt
// the participant continues with. Every outcome lets the participant continue:
// a poor result, a skipped calibration and an unavailable camera included.
// The set-up screen is the participant interface's CalibrationView; only the
// nine-point run itself is a full-screen overlay.

type CalibrationStepProps = {
  eyeTracking: ParticipantEyeTracking | null
  isCompleting: boolean
  errorMessage: string | null
  onComplete: (attempt: CalibrationAttempt) => Promise<boolean>
}

const UNAVAILABLE_REASONS: Record<EyeTrackingUnavailableReason, string> = {
  NO_CAMERA_API: 'This browser does not allow camera access on this page.',
  NO_CAMERA_DEVICE: 'No camera was found.',
  PERMISSION_DENIED: 'Camera access was blocked.',
  INIT_FAILED: 'The eye tracker could not be started.',
  INIT_CANCELLED: 'Calibration was skipped before the camera started.',
  INIT_TIMEOUT: 'The camera took too long to start.',
}

function Notice({ title, children, live = false }: { title: string; children?: ReactNode; live?: boolean }) {
  return (
    <div className="demo-notice">
      <div role={live ? 'status' : undefined}>
        <strong>{title}</strong>
        {children}
      </div>
    </div>
  )
}

const noop = () => {}

function CalibrationStep({ eyeTracking, isCompleting, errorMessage, onComplete }: CalibrationStepProps) {
  const snapshot = useEyeTrackingSnapshot(eyeTracking)

  if (!eyeTracking || !snapshot) {
    return (
      <CalibrationView
        onNext={noop}
        pending
        previewStatus="Preparing"
        previewCaption="Camera is not active"
        notice={<Notice title="Preparing eye tracking..." live />}
      />
    )
  }

  switch (snapshot.stage) {
    case 'IDLE':
      return (
        <CalibrationView
          onNext={() => void eyeTracking.calibrate()}
          nextLabel="Start calibration"
          previewStatus="Off"
          previewCaption="The camera starts when you begin calibration"
          notice={
            <Notice title="Your video stays in this browser">
              <p>
                This study uses your webcam to estimate where you look on the screen. The video is
                never uploaded; only the estimated screen positions are saved with your responses.
              </p>
              <p>
                Nine dots appear one at a time. Look at each dot and click it five times. If your
                camera is unavailable, you can still continue the study.
              </p>
            </Notice>
          }
        />
      )
    case 'STARTING':
      return (
        <CalibrationView
          onNext={() => eyeTracking.skip()}
          nextLabel="Skip calibration"
          previewStatus="Starting"
          previewCaption="Allow camera access if your browser asks"
          notice={
            <Notice title="Starting the camera..." live>
              <p>This can take a few seconds.</p>
            </Notice>
          }
        />
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
        <CalibrationView
          onNext={noop}
          pending
          previewStatus="Off"
          previewCaption="Camera is not active"
          notice={<Notice title="Continuing to the study..." live />}
        />
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

function resultText(attempt: CalibrationAttempt): { title: string; detail: string; status: string } {
  if (!attempt.trackingAvailable) {
    const reason = attempt.unavailableReason
      ? UNAVAILABLE_REASONS[attempt.unavailableReason]
      : UNAVAILABLE_REASONS.INIT_FAILED
    return {
      title: 'Eye tracking is not available',
      detail: `${reason} You can continue the study without eye tracking.`,
      status: 'Unavailable',
    }
  }
  if (attempt.outcome === 'ABANDONED') {
    return {
      title: 'Calibration was not completed',
      detail: 'Eye tracking will continue, but its accuracy will be lower.',
      status: 'Partial',
    }
  }
  switch (attempt.quality) {
    case 'GOOD':
      return { title: 'Calibration complete', detail: 'Accuracy looks good.', status: 'Good' }
    case 'FAIR':
      return { title: 'Calibration complete', detail: 'Accuracy is acceptable.', status: 'Fair' }
    case 'POOR':
      return { title: 'Calibration complete', detail: 'Accuracy is low.', status: 'Low accuracy' }
    default:
      return { title: 'Calibration complete', detail: 'Its accuracy could not be measured.', status: 'Not measured' }
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
  const { title, detail, status } = resultText(attempt)

  return (
    <CalibrationView
      onNext={() => void onComplete(attempt)}
      nextLabel={isCompleting ? 'Saving...' : 'Continue'}
      pending={isCompleting}
      previewStatus={status}
      previewCaption={attempt.trackingAvailable
        ? 'Your camera stays on until you finish browsing'
        : 'Camera is not active'}
      notice={
        <Notice title={title}>
          <p>{detail}</p>
          {snapshot.canRecalibrate && (
            <>
              <p>You can recalibrate to improve accuracy, or continue now.</p>
              <button
                type="button"
                className="demo-button demo-button--quiet mt-3"
                disabled={isCompleting}
                onClick={() => void eyeTracking.calibrate()}
              >
                Recalibrate
              </button>
            </>
          )}
          {errorMessage && (
            <p className="mt-3 font-semibold text-red-700" role="alert">
              {errorMessage}
            </p>
          )}
        </Notice>
      }
    />
  )
}

export default CalibrationStep
