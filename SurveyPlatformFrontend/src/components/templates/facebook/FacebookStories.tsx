const stories = [
  {
    initials: 'AC',
    name: 'Alex',
    background: 'from-cyan-400 to-cyan-600',
  },
  {
    initials: 'JL',
    name: 'Jordan',
    background: 'from-emerald-400 to-emerald-600',
  },
  {
    initials: 'MT',
    name: 'Mia',
    background: 'from-orange-300 to-orange-500',
  },
  {
    initials: 'SK',
    name: 'Sam',
    background: 'from-rose-400 to-rose-600',
  },
]

function FacebookStories() {
  return (
    <section className="mx-auto w-full max-w-[580px]">
      <div className="flex justify-center gap-3 overflow-x-auto pb-2">
        {/* Create story */}
        <article className="relative h-36 min-w-[92px] overflow-hidden rounded-xl bg-white shadow-sm">
          <div className="h-[68%] bg-gray-200" />

          <div className="absolute left-1/2 top-[57%] flex h-9 w-9 -translate-x-1/2 items-center justify-center rounded-full border-4 border-white bg-blue-600 text-base font-bold text-white">
            +
          </div>

          <div className="flex h-[32%] items-end justify-center px-2 pb-3">
            <span className="text-center text-xs font-semibold leading-4 text-gray-900">
              Create story
            </span>
          </div>
        </article>

        {/* Existing stories */}
        {stories.map((story) => (
          <article
            key={story.name}
            className={`relative h-36 min-w-[92px] overflow-hidden rounded-xl bg-gradient-to-b ${story.background} shadow-sm`}
          >
            <div className="absolute left-3 top-3 flex h-9 w-9 items-center justify-center rounded-full border-4 border-blue-500 bg-white text-xs font-semibold text-gray-800">
              {story.initials}
            </div>

            <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/60 to-transparent px-3 pb-3 pt-10">
              <span className="text-sm font-semibold text-white">
                {story.name}
              </span>
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}

export default FacebookStories