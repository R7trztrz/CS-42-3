import { Outlet, useParams } from 'react-router-dom'
import { SessionProvider, useSession } from '../state/SessionContext'

// UC-28: entering via a study's participation link creates the session.
// Real link/token handling (FR-41, NFR-09 token security) belongs to
// whoever builds the M5 backend; this just takes studyId from the route.
function SessionGate({ children }: { children: React.ReactNode }) {
  const { isLoading, error } = useSession()

  if (isLoading) {
    return (
      <div className="mx-auto max-w-xl px-4 py-16 text-center text-sm text-gray-500">
        Setting up your session...
      </div>
    )
  }

  if (error) {
    return (
      <div className="mx-auto max-w-xl px-4 py-16 text-center text-sm text-red-600">{error}</div>
    )
  }

  return <>{children}</>
}

export default function ParticipantSessionLayout() {
  const { studyId } = useParams<{ studyId: string }>()

  if (!studyId) {
    return (
      <div className="mx-auto max-w-xl px-4 py-16 text-center text-sm text-red-600">
        Missing study identifier.
      </div>
    )
  }

  return (
    <SessionProvider studyId={studyId}>
      <div className="min-h-screen bg-gray-50">
        <SessionGate>
          <Outlet />
        </SessionGate>
      </div>
    </SessionProvider>
  )
}
