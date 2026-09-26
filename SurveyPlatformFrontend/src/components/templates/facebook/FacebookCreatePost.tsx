function FacebookCreatePost() {
  return (
    <section className="w-full max-w-xl rounded-xl bg-white p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-indigo-600 text-sm font-semibold text-white">
          You
        </div>

        <button
          type="button"
          className="flex-1 rounded-full bg-gray-100 px-4 py-2.5 text-left text-sm text-gray-500 hover:bg-gray-200"
        >
          What's on your mind?
        </button>
      </div>

      <div className="mt-4 border-t border-gray-200 pt-3">
        <div className="grid grid-cols-3 gap-2">
          <button
            type="button"
            className="flex items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-gray-600 hover:bg-gray-100"
          >
            <span>🎥</span>
            <span>Live video</span>
          </button>

          <button
            type="button"
            className="flex items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-gray-600 hover:bg-gray-100"
          >
            <span>🖼️</span>
            <span>Photo/video</span>
          </button>

          <button
            type="button"
            className="flex items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-gray-600 hover:bg-gray-100"
          >
            <span>😊</span>
            <span>Feeling</span>
          </button>
        </div>
      </div>
    </section>
  )
}

export default FacebookCreatePost