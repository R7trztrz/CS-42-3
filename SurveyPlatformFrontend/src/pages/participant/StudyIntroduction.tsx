import type { ParticipationResponse } from '../../services/studyApi'
import { IntroductionStep } from './ParticipantDemoTemplate'
import ParticipationFlowLayout from './ParticipationFlowLayout'

type Props = {
  study: ParticipationResponse
  pending: boolean
  onStart: () => void
}

export default function StudyIntroduction({ study, pending, onStart }: Props) {
  return (
    <ParticipationFlowLayout step="introduction">
      <IntroductionStep study={study} pending={pending} onNext={onStart} />
    </ParticipationFlowLayout>
  )
}
