import { useNode } from '@craftjs/core'


const stories = [
  {
    name: 'Emma',
    initials: 'EW',
    bg: 'bg-indigo-600',
  },
  {
    name: 'Alex',
    initials: 'AC',
    bg: 'bg-cyan-600',
  },
  {
    name: 'Jordan',
    initials: 'JL',
    bg: 'bg-emerald-600',
  },
  {
    name: 'Mia',
    initials: 'MT',
    bg: 'bg-orange-500',
  },
  {
    name: 'Sam',
    initials: 'SK',
    bg: 'bg-red-500',
  },
  {
    name: 'Your story',
    initials: '+',
    bg: 'bg-gray-500',
  },
]


function InstagramStories() {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <section
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="mb-4 overflow-hidden border-b border-[#efefef] pb-3"
    >
      <div className="flex justify-start gap-5 overflow-x-auto px-1">

        {stories.map(
          ({
            name,
            initials,
            bg,
          }) => (
            <div
              key={name}
              className="flex min-w-[60px] flex-col items-center gap-1"
            >
              <div className="rounded-full bg-gradient-to-br from-purple-600 via-pink-500 to-orange-400 p-[2px]">

                <div className="rounded-full bg-white p-[2px]">

                  <div
                    className={`flex h-[50px] w-[50px] items-center justify-center rounded-full text-[11px] font-bold text-white ${bg}`}
                  >
                    {initials}
                  </div>

                </div>

              </div>

              <span className="max-w-[60px] truncate text-[10px] text-[#262626]">
                {name}
              </span>
            </div>
          ),
        )}

      </div>
    </section>
  )
}


InstagramStories.craft = {
  displayName: 'Instagram Stories',
}


export default InstagramStories