import { Navigate } from 'react-router-dom'
import { useSession } from '../state/SessionContext'

// UC-28/UC-32: routes the participant to wherever they left off. Acts as
// both the fresh-entry landing point and the resume point after a reload.
export default function SessionEntryPage() {
  const { hasConsented, hasSeenFeed, isComplete } = useSession()

  if (!hasConsented) return <Navigate to="consent" replace />
  if (!hasSeenFeed) return <Navigate to="feed" replace />
  if (!isComplete) return <Navigate to="questionnaire" replace />
  return <Navigate to="complete" replace />
}
