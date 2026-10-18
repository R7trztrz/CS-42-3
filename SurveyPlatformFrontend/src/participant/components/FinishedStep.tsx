import type {
  ParticipantAbandonmentReason,
  ParticipantSessionStatus,
} from '../model/participantSession'

type FinishedStepProps = {
  status: ParticipantSessionStatus
  abandonmentReason: ParticipantAbandonmentReason
}

function describeOutcome(
  status: ParticipantSessionStatus,
  reason: ParticipantAbandonmentReason,
): { title: string; detail: string } {
  if (status === 'COMPLETED') {
    return {
      title: 'Thank you for participating',
      detail: 'Your responses have been recorded. You may now close this page.',
    }
  }

  switch (reason) {
    case 'CONSENT_DECLINED':
      return {
        title: 'Thanks for your time',
        detail: 'You chose not to take part in this study. No data has been recorded.',
      }
    case 'PARTICIPANT_EXIT':
      return {
        title: 'You have left the study',
        detail: 'Your session has ended. You may now close this page.',
      }
    case 'INACTIVITY_TIMEOUT':
      return {
        title: 'Session timed out',
        detail: 'This session ended due to inactivity. You may now close this page.',
      }
    case 'STUDY_CLOSED':
      return {
        title: 'This study is closed',
        detail: 'The researcher has closed this study to new or continuing participants.',
      }
    default:
      return {
        title: 'This session has ended',
        detail: 'You may now close this page.',
      }
  }
}

function FinishedStep({ status, abandonmentReason }: FinishedStepProps) {
  const { title, detail } = describeOutcome(status, abandonmentReason)

  return (
    <section className="demo-complete">
      {status === 'COMPLETED' && <div className="demo-complete__mark" aria-hidden="true" />}
      <p className="demo-eyebrow">{status === 'COMPLETED' ? 'Study complete' : 'Session ended'}</p>
      <h2 className="text-xl font-semibold text-[#172033]">{title}</h2>
      <p className="mt-2 text-sm text-gray-600">{detail}</p>
    </section>
  )
}

export default FinishedStep
