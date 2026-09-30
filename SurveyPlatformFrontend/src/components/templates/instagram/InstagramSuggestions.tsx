import { useNode } from '@craftjs/core'


const suggestions = [
  {
    username: '@emmaw',
    name: 'Emma Wilson',
    initials: 'EW',
    bg: 'bg-indigo-600',
  },
  {
    username: '@alexc',
    name: 'Alex Chen',
    initials: 'AC',
    bg: 'bg-cyan-600',
  },
  {
    username: '@jordanl',
    name: 'Jordan Lee',
    initials: 'JL',
    bg: 'bg-emerald-600',
  },
  {
    username: '@miat',
    name: 'Mia Taylor',
    initials: 'MT',
    bg: 'bg-orange-500',
  },
  {
    username: '@samk',
    name: 'Sam Kim',
    initials: 'SK',
    bg: 'bg-red-500',
  },
]


function InstagramSuggestions() {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <aside
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="w-[270px] shrink-0 px-5 py-7"
    >
      <div className="mb-7 flex items-center gap-3">

        <div className="rounded-full bg-gradient-to-br from-purple-600 via-pink-500 to-orange-400 p-[2px]">
          <div className="flex h-11 w-11 items-center justify-center rounded-full bg-white text-[10px] font-semibold text-gray-700">
            You
          </div>
        </div>

        <div className="min-w-0 flex-1">
          <p className="text-xs font-semibold text-[#262626]">
            yourprofile
          </p>

          <p className="text-[11px] text-[#8e8e8e]">
            Your account
          </p>
        </div>

        <button
          type="button"
          className="text-[11px] font-semibold text-[#0095f6]"
        >
          Switch
        </button>
      </div>


      <div className="mb-4 flex items-center justify-between">

        <p className="text-[11px] font-semibold text-[#8e8e8e]">
          Suggested for you
        </p>

        <button
          type="button"
          className="text-[11px] font-semibold text-[#262626]"
        >
          See all
        </button>

      </div>


      <div className="space-y-4">

        {suggestions.map(
          ({
            username,
            name,
            initials,
            bg,
          }) => (
            <div
              key={username}
              className="flex items-center gap-3"
            >
              <div
                className={`flex h-8 w-8 items-center justify-center rounded-full text-[9px] font-bold text-white ${bg}`}
              >
                {initials}
              </div>

              <div className="min-w-0 flex-1">

                <p className="truncate text-[11px] font-semibold text-[#262626]">
                  {username}
                </p>

                <p className="truncate text-[10px] text-[#8e8e8e]">
                  {name}
                </p>

              </div>

              <button
                type="button"
                className="text-[10px] font-semibold text-[#0095f6]"
              >
                Follow
              </button>
            </div>
          ),
        )}

      </div>


      <p className="mt-8 text-[9px] leading-4 text-[#c7c7c7]">
        This is a simulated research prototype.
        All accounts and content are fictional.
      </p>
    </aside>
  )
}


InstagramSuggestions.craft = {
  displayName:
    'Instagram Suggestions',
}


export default InstagramSuggestions