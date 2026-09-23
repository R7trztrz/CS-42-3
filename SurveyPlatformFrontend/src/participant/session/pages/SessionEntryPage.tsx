import { Navigate } from 'react-router-dom'
import { useSession } from '../state/SessionContext'

// UC-28/UC-32：把参与者带到上次离开的地方。既是首次进入的落地页，
// 也是刷新页面后的续传入口。
export default function SessionEntryPage() {
  const { hasConsented, hasSeenFeed, isComplete } = useSession()

  if (!hasConsented) return <Navigate to="consent" replace />
  if (!hasSeenFeed) return <Navigate to="feed" replace />
  if (!isComplete) return <Navigate to="questionnaire" replace />
  return <Navigate to="complete" replace />
}
