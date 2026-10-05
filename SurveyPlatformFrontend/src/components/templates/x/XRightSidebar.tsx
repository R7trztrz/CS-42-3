import {
  useNode,
} from '@craftjs/core'

import XIcon from './XIcon'

const trends = [
  {
    meta: 'Trending in Technology',
    title: '#ResearchPrototype',
    count: '12,400 posts',
  },
  {
    meta: 'Science · Trending',
    title: '#SocialMediaStudy',
    count: '8,300 posts',
  },
  {
    meta: 'Trending in HCI',
    title: '#HCI2026',
    count: '5,100 posts',
  },
  {
    meta: 'Technology · Trending',
    title: '#UXResearch',
    count: '3,200 posts',
  },
]

const suggestions = [
  {
    initials: 'EW',
    name: 'Emma Wilson',
    handle: '@emmaw',
    background: '#5146e5',
  },
  {
    initials: 'AC',
    name: 'Alex Chen',
    handle: '@alexc',
    background: '#11a4c8',
  },
  {
    initials: 'JL',
    name: 'Jordan Lee',
    handle: '@jordanl',
    background: '#00a87f',
  },
]

function XRightSidebar() {
  const {
    connectors: {
      connect,
    },
  } = useNode()

  return (
    <aside
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="h-full w-[360px] shrink-0 overflow-y-auto bg-black px-5 py-3"
    >
      <div className="flex h-[48px] items-center gap-3 rounded-full bg-[#202327] px-5 text-[#71767b]">
        <XIcon
          name="search"
          size={20}
        />

        <span className="text-[15px]">
          Search
        </span>
      </div>

      <section className="mt-4 overflow-hidden rounded-[18px] border border-[#2f3336] bg-[#16181c]">
        <h2 className="border-b border-[#2f3336] px-5 py-4 text-[20px] font-extrabold">
          What's happening
        </h2>

        {trends.map(
          (trend) => (
            <div
              key={trend.title}
              className="border-b border-[#2f3336] px-5 py-4 last:border-b-0"
            >
              <p className="text-[13px] text-[#71767b]">
                {trend.meta}
              </p>

              <p className="mt-1 text-[15px] font-bold text-white">
                {trend.title}
              </p>

              <p className="mt-1 text-[13px] text-[#71767b]">
                {trend.count}
              </p>
            </div>
          ),
        )}

        <div className="px-5 py-4 text-[14px] text-[#1d9bf0]">
          Show more
        </div>
      </section>

      <section className="mt-5 overflow-hidden rounded-[18px] border border-[#2f3336] bg-[#16181c]">
        <h2 className="border-b border-[#2f3336] px-5 py-4 text-[20px] font-extrabold">
          Who to follow
        </h2>

        {suggestions.map(
          (person) => (
            <div
              key={person.handle}
              className="flex items-center gap-3 border-b border-[#2f3336] px-5 py-4 last:border-b-0"
            >
              <div
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-[12px] font-bold text-white"
                style={{
                  background:
                    person.background,
                }}
              >
                {person.initials}
              </div>

              <div className="min-w-0 flex-1">
                <p className="truncate text-[14px] font-bold">
                  {person.name}
                </p>

                <p className="truncate text-[12px] text-[#71767b]">
                  {person.handle}
                </p>
              </div>

              <button
                type="button"
                className="rounded-full bg-white px-5 py-2 text-[13px] font-bold text-black"
              >
                Follow
              </button>
            </div>
          ),
        )}

        <div className="px-5 py-4 text-[14px] text-[#1d9bf0]">
          Show more
        </div>
      </section>
    </aside>
  )
}

XRightSidebar.craft = {
  displayName:
    'X Right Sidebar',
}

export default XRightSidebar