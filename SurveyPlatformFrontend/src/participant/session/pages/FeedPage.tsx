import { useNavigate } from 'react-router-dom'
import { useSession } from '../state/SessionContext'

// UC-30: browse the simulated feed, then trigger the transition to the
// questionnaire. Feed *content* comes from M3, which isn't built yet - this
// renders generic placeholder posts so the flow and behavioural-data hooks
// (e.g. where a click/dwell-time tracker would attach) can be previewed.
export default function FeedPage() {
  const { feed, markFeedSeen } = useSession()
  const navigate = useNavigate()

  function handleContinue() {
    markFeedSeen()
    navigate('../questionnaire', { replace: true })
  }

  return (
    <div className="mx-auto max-w-xl px-4 py-8">
      <h1 className="mb-1 text-2xl font-semibold text-gray-900">Your feed</h1>
      <p className="mb-6 text-sm text-gray-500">
        Scroll through the feed below, then continue when you're ready.
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
        Continue to questionnaire
      </button>
    </div>
  )
}
