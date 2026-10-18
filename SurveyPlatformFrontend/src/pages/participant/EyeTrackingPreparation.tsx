import CameraPreview from '../../participant/components/CameraPreview'
import { CalibrationView } from './ParticipantDemoTemplate'

type Props = {
  enabled: boolean
  confirmed: boolean
  /** M6: true while the calibrated camera stays on for the activity. */
  cameraOn?: boolean
  /** M6: that camera's stream, shown in the preview panel while it is on. */
  previewStream?: MediaStream | null
  onContinue: () => void
}

function previewFor(enabled: boolean, confirmed: boolean, cameraOn: boolean | undefined) {
  if (enabled && confirmed && cameraOn === true) {
    return { status: 'On', caption: 'Your camera stays on until you finish browsing' }
  }
  if (enabled && confirmed && cameraOn === false) {
    return { status: 'Off', caption: 'Camera is not active on this screen' }
  }
  return { status: enabled && confirmed ? 'Confirmed' : 'Not active', caption: 'Camera is not active on this screen' }
}

function message(enabled: boolean, confirmed: boolean, cameraOn: boolean | undefined) {
  if (!enabled) {
    return {
      title: 'Before the activity',
      body: 'Eye tracking is not enabled for this study. No camera access or calibration is required.',
    }
  }
  if (!confirmed) {
    return {
      title: 'Before the activity',
      body: 'The study session is ready for browsing. No camera is active on this preparation screen.',
    }
  }
  if (cameraOn) {
    return {
      title: 'Calibration confirmed',
      body: 'Eye tracking runs while you browse the feed and stops when you finish. '
        + 'The video stays in this browser; only estimated screen positions are saved.',
    }
  }
  if (cameraOn === false) {
    return {
      title: 'Eye tracking is off for this session',
      body: 'The camera is not running, so no eye-tracking data will be collected. You can continue to the activity.',
    }
  }
  return {
    title: 'Calibration confirmed',
    body: 'Your calibration has been confirmed. You can continue to the activity.',
  }
}

export default function EyeTrackingPreparation({ enabled, confirmed, cameraOn, previewStream, onContinue }: Props) {
  const preview = previewFor(enabled, confirmed, cameraOn)
  const { title, body } = message(enabled, confirmed, cameraOn)
  return (
    <CalibrationView
      onNext={onContinue}
      nextLabel="Continue to activity"
      previewStatus={preview.status}
      previewCaption={preview.caption}
      preview={cameraOn && previewStream ? <CameraPreview stream={previewStream} /> : undefined}
      notice={
        <div className="demo-notice">
          <div>
            <strong>{title}</strong>
            <p>{body}</p>
          </div>
        </div>
      }
    />
  )
}
