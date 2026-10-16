import { useParams, useSearchParams } from 'react-router-dom'
import StaticParticipantDemo from './ParticipantDemoTemplate'
import ParticipationPage from './ParticipationPage'

export default function ParticipantDemo() {
  const { token } = useParams<{ token: string }>()
  const [searchParams] = useSearchParams()
  const studyToken = token ?? searchParams.get('token')

  return studyToken !== null && studyToken !== undefined
    ? <ParticipationPage key={studyToken} studyToken={studyToken} />
    : <StaticParticipantDemo />
}
