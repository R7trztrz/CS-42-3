import { CalibrationView } from './ParticipantDemoTemplate'

type Props = {
  enabled: boolean
  confirmed: boolean
  onContinue: () => void
}

export default function EyeTrackingPreparation({ enabled, confirmed, onContinue }: Props) {
  return (
    <CalibrationView
      onNext={onContinue}
      nextLabel="Continue to activity"
      previewStatus={enabled && confirmed ? 'Confirmed' : 'Not active'}
      previewCaption="Camera is not active on this screen"
      notice={
        <div className="demo-notice">
          <div>
            <strong>{enabled && confirmed ? 'Calibration confirmed' : 'Before the activity'}</strong>
            <p>{enabled && confirmed
              ? 'Your calibration has been confirmed. You can continue to the activity.'
              : enabled
                ? 'The study session is ready for browsing. No camera is active on this preparation screen.'
                : 'Eye tracking is not enabled for this study. No camera access or calibration is required.'}</p>
          </div>
        </div>
      }
    />
  )
}
