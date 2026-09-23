import { Outlet, useParams } from 'react-router-dom'
import { SessionProvider, useSession } from '../state/SessionContext'

// UC-28：通过参与链接进入研究即创建会话。真实的链接/token 处理
// （FR-41、NFR-09 令牌安全）留给负责搭建 M5 后端的同学，
// 这里只是简单地从路由里取 studyId。
function SessionGate({ children }: { children: React.ReactNode }) {
  const { isLoading, error } = useSession()

  if (isLoading) {
    return (
      <div className="mx-auto max-w-xl px-4 py-16 text-center text-sm text-gray-500">
        正在准备你的会话...
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
        缺少研究标识。
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
