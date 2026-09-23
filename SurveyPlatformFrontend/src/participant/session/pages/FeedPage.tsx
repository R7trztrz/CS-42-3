import { useNavigate } from 'react-router-dom'
import { useSession } from '../state/SessionContext'

// UC-30：浏览模拟信息流，浏览完后触发进入问卷。信息流的*内容*来自 M3，
// 目前还没做 —— 这里渲染的是通用占位帖子，方便预览流程本身，以及日后
// 行为数据采集（比如点击/停留时长埋点）能挂在哪里。
export default function FeedPage() {
  const { feed, markFeedSeen } = useSession()
  const navigate = useNavigate()

  function handleContinue() {
    markFeedSeen()
    navigate('../questionnaire', { replace: true })
  }

  return (
    <div className="mx-auto max-w-xl px-4 py-8">
      <h1 className="mb-1 text-2xl font-semibold text-gray-900">你的信息流</h1>
      <p className="mb-6 text-sm text-gray-500">
        向下浏览下面的信息流，准备好之后点击继续。
      </p>

      <div className="space-y-3">
        {feed.map((post) => (
          <div key={post.id} className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
            <p className="text-xs font-medium text-gray-400">
              {post.platform} · {post.author}
            </p>
            <p className="mt-1 text-sm text-gray-800">{post.content}</p>
          </div>
        ))}
      </div>

      <button
        type="button"
        onClick={handleContinue}
        className="mt-6 w-full rounded-lg bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700"
      >
        继续进入问卷
      </button>
    </div>
  )
}
