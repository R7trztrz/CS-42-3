import { useNode } from '@craftjs/core'


const stories = [
  {
    name: 'Alex',
    initials: 'AC',
    bg:
      'bg-gradient-to-b from-cyan-400 to-cyan-700',
  },
  {
    name: 'Jordan',
    initials: 'JL',
    bg:
      'bg-gradient-to-b from-emerald-400 to-emerald-700',
  },
  {
    name: 'Mia',
    initials: 'MT',
    bg:
      'bg-gradient-to-b from-orange-300 to-orange-700',
  },
  {
    name: 'Sam',
    initials: 'SK',
    bg:
      'bg-gradient-to-b from-rose-400 to-rose-800',
  },
]


function FacebookStories() {
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
      className="mx-auto mb-4 w-full max-w-[560px]"
    >
      <div className="flex gap-2 overflow-x-auto pb-2">

        {/* Create story */}
        <div className="flex h-[150px] min-w-[94px] flex-col overflow-hidden rounded-xl bg-white shadow-sm">

          <div className="flex flex-1 items-end justify-center bg-[#e4e6eb] pb-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-full border-4 border-white bg-[#1877f2] text-xl font-semibold text-white">
              +
            </div>
          </div>

          <div className="flex h-[40px] items-center justify-center px-2 text-center">
            <span className="text-[11px] font-semibold text-[#050505]">
              Create story
            </span>
          </div>

        </div>


        {stories.map(
          ({
            name,
            initials,
            bg,
          }) => (
            <div
              key={name}
              className={`relative h-[150px] min-w-[94px] overflow-hidden rounded-xl shadow-sm ${bg}`}
            >
              <div className="absolute left-2 top-2 rounded-full border-[3px] border-[#1877f2] bg-white p-[2px]">

                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-[#f0f2f5] text-[10px] font-bold text-[#172033]">
                  {initials}
                </div>

              </div>

              <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/70 to-transparent px-2 pb-3 pt-8">

                <span className="text-[11px] font-semibold text-white">
                  {name}
                </span>

              </div>
            </div>
          ),
        )}

      </div>
    </section>
  )
}


FacebookStories.craft = {
  displayName:
    'Facebook Stories',
}


export default FacebookStories