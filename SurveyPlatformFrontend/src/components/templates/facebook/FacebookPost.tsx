type FacebookPostProps = {
  author?: string
  time?: string
  text?: string
  likes?: number
  comments?: number
  shares?: number
}

function FacebookPost({
  author = 'Emma Wilson',
  time = '2h ago',
  text = 'Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂',
  likes = 47,
  comments = 12,
  shares = 8,
}: FacebookPostProps) {
  const initials = author
    .split(' ')
    .map((name) => name.charAt(0))
    .join('')
    .slice(0, 2)
    .toUpperCase()

  return (
    <article className="w-full max-w-xl overflow-hidden rounded-xl bg-white shadow-sm">
      {/* Post header */}
      <div className="flex items-start justify-between px-4 pt-4">
        <div className="flex items-center gap-3">
          {/* Avatar placeholder */}
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-indigo-600 text-sm font-semibold text-white">
            {initials}
          </div>

          <div>
            <div className="flex items-center gap-1">
              <p className="font-semibold text-gray-900">
                {author}
              </p>

              <span
                className="flex h-4 w-4 items-center justify-center rounded-full bg-blue-500 text-[10px] font-bold text-white"
                aria-label="Verified account"
              >
                ✓
              </span>
            </div>

            <div className="mt-0.5 flex items-center gap-1 text-xs text-gray-500">
              <span>{time}</span>
              <span>·</span>
              <span aria-label="Public post">🌐</span>
            </div>
          </div>
        </div>

        {/* Post options */}
        <button
          type="button"
          className="flex h-9 w-9 items-center justify-center rounded-full text-xl leading-none text-gray-500 transition hover:bg-gray-100 hover:text-gray-700"
          aria-label="Post options"
        >
          ⋯
        </button>
      </div>

      {/* Post text */}
      <div className="px-4 py-3">
        <p className="text-sm leading-6 text-gray-900">
          {text}
        </p>
      </div>

      {/* Image placeholder */}
      <div className="flex h-80 w-full items-center justify-center bg-gray-100">
        <div className="text-center">
          <div className="mb-2 text-3xl text-gray-400">
            🖼️
          </div>

          <p className="text-sm font-medium text-gray-500">
            Image placeholder
          </p>
        </div>
      </div>

      {/* Engagement summary */}
      <div className="flex items-center justify-between px-4 py-3 text-sm text-gray-500">
        <div className="flex items-center gap-2">
          <div className="flex -space-x-1">
            <div className="flex h-5 w-5 items-center justify-center rounded-full bg-blue-500 text-[10px] text-white ring-2 ring-white">
              👍
            </div>

            <div className="flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-[10px] text-white ring-2 ring-white">
              ♥
            </div>
          </div>

          <span>{likes}</span>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            className="hover:underline"
          >
            {comments} comments
          </button>

          <button
            type="button"
            className="hover:underline"
          >
            {shares} shares
          </button>
        </div>
      </div>

      {/* Divider */}
      <div className="mx-4 border-t border-gray-200" />

      {/* Action buttons */}
      <div className="grid grid-cols-3 px-2 py-1">
        <button
          type="button"
          className="flex items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-gray-600 transition hover:bg-gray-100"
        >
          <span>👍</span>
          <span>Like</span>
        </button>

        <button
          type="button"
          className="flex items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-gray-600 transition hover:bg-gray-100"
        >
          <span>💬</span>
          <span>Comment</span>
        </button>

        <button
          type="button"
          className="flex items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-gray-600 transition hover:bg-gray-100"
        >
          <span>↗</span>
          <span>Share</span>
        </button>
      </div>
    </article>
  )
}

FacebookPost.craft = {
  displayName: 'Facebook Post',
  props: {
    author: 'Emma Wilson',
    time: '2h ago',
    text: 'Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂',
    likes: 47,
    comments: 12,
    shares: 8,
  },
}

export default FacebookPost