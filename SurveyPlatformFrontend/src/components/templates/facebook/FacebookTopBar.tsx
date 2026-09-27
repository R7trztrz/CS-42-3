function FacebookTopBar() {
  return (
    <header className="sticky top-0 z-20 border-b border-gray-200 bg-white">
      <div className="flex h-16 items-center justify-between px-4">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-600 text-xl font-bold text-white">
            f
          </div>

          <div className="hidden w-60 items-center rounded-full bg-gray-100 px-4 py-2 md:flex">
            <span className="mr-2 text-gray-400">⌕</span>
            <span className="text-sm text-gray-500">
              Search Facebook
            </span>
          </div>
        </div>

        <nav className="hidden flex-1 items-center justify-center gap-10 md:flex">
          <button
            type="button"
            className="flex h-12 w-16 items-center justify-center border-b-4 border-blue-600 text-xl text-blue-600"
            aria-label="Home"
          >
            ⌂
          </button>

          <button
            type="button"
            className="flex h-12 w-16 items-center justify-center text-xl text-gray-500 hover:bg-gray-100"
            aria-label="Friends"
          >
            👥
          </button>

          <button
            type="button"
            className="flex h-12 w-16 items-center justify-center text-xl text-gray-500 hover:bg-gray-100"
            aria-label="Video"
          >
            ▷
          </button>

          <button
            type="button"
            className="flex h-12 w-16 items-center justify-center text-xl text-gray-500 hover:bg-gray-100"
            aria-label="Marketplace"
          >
            🛍
          </button>

          <button
            type="button"
            className="flex h-12 w-16 items-center justify-center text-xl text-gray-500 hover:bg-gray-100"
            aria-label="Gaming"
          >
            🎮
          </button>
        </nav>

        <div className="flex items-center gap-2">
          <button
            type="button"
            className="flex h-10 w-10 items-center justify-center rounded-full bg-gray-200 text-gray-700 hover:bg-gray-300"
            aria-label="Messages"
          >
            ●
          </button>

          <button
            type="button"
            className="flex h-10 w-10 items-center justify-center rounded-full bg-gray-200 text-gray-700 hover:bg-gray-300"
            aria-label="Notifications"
          >
            🔔
          </button>

          <button
            type="button"
            className="flex h-10 w-10 items-center justify-center rounded-full bg-gray-200 text-gray-700 hover:bg-gray-300"
            aria-label="More"
          >
            ⋯
          </button>

          <button
            type="button"
            className="flex h-10 w-10 items-center justify-center rounded-full bg-indigo-600 text-xs font-semibold text-white"
            aria-label="Profile"
          >
            You
          </button>
        </div>
      </div>
    </header>
  )
}

export default FacebookTopBar